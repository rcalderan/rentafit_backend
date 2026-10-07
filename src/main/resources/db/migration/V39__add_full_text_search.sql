-- V39: Full-Text Search (to_tsvector/to_tsquery + GIN) para clientes, produtos e contratos.
--
-- Config pt_unaccent = COPY de 'portuguese' (stemming + stopwords) com unaccent no
-- mapping — nomes BR ("josé" casa "jose") e plurais ("cadernos" casa "caderno")
-- numa passada só, sem custo extra por consulta.
--
-- As funções *_search_vec são a FONTE ÚNICA da expressão indexada: o índice GIN e o
-- JPQL (via function()) chamam a mesma função. LANGUAGE sql IMMUTABLE → o planner
-- inline o corpo, então custo é idêntico a escrever a expressão inline — mas sem
-- risco de drift índice↔query (drift vira seq scan silencioso).
-- Não são STRICT: coalesce interno garante vetor não-nulo mesmo com email/details NULL.
-- fts_match/fts_rank existem porque JPQL não emite o operador @@.
-- translate('-',' ') antes de vetorizar: o stemmer português deforma a parte após
-- hífen ("SKU-98765" → '-98765'), quebrando busca por identificador e nome hifenado.
-- ATENÇÃO: alterar o corpo de qualquer *_search_vec exige recriar os índices GIN
-- correspondentes — o índice materializa os vetores com a função da época.

CREATE EXTENSION IF NOT EXISTS unaccent;

DROP TEXT SEARCH CONFIGURATION IF EXISTS pt_unaccent;
CREATE TEXT SEARCH CONFIGURATION pt_unaccent (COPY = portuguese);
ALTER TEXT SEARCH CONFIGURATION pt_unaccent
    ALTER MAPPING FOR hword, hword_part, word WITH unaccent, portuguese_stem;

-- Lexemes crus (sem stemming), só unaccent+lower: "claudio" ≠ "claudiane",
-- mas "josé" ainda casa "jose". Usado só no boost de match exato do ranking —
-- claudio e claudiane têm o mesmo stem 'claudi' e sem isso empatam no rank.
DROP TEXT SEARCH CONFIGURATION IF EXISTS raw_unaccent;
CREATE TEXT SEARCH CONFIGURATION raw_unaccent (COPY = simple);
ALTER TEXT SEARCH CONFIGURATION raw_unaccent
    ALTER MAPPING FOR hword, hword_part, word WITH unaccent, simple;

CREATE OR REPLACE FUNCTION fts_match(tsvector, tsquery) RETURNS boolean
    LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE AS $$ SELECT $1 @@ $2 $$;

CREATE OR REPLACE FUNCTION fts_rank(tsvector, tsquery) RETURNS real
    LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE AS $$ SELECT ts_rank($1, $2) $$;

-- $2 = tsquery pt_unaccent (match, com stemming/prefixo); $3 = tsquery raw_unaccent
-- com tokens exatos ORed. Match exato por token pesa 10x sobre o rank comum.
CREATE OR REPLACE FUNCTION fts_rank_boosted(tsvector, tsquery, tsquery) RETURNS real
    LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE AS $$
    SELECT ts_rank($1, $2) + 10.0::real * ts_rank($1, $3)
$$;

-- nome A / email C — cada campo entra também em raw_unaccent no mesmo peso
-- (alimenta o boost de match exato de fts_rank_boosted).
CREATE OR REPLACE FUNCTION customer_search_vec(text, text) RETURNS tsvector
    LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT setweight(to_tsvector('pt_unaccent', coalesce(translate($1, '-', ' '), '')), 'A')
        || setweight(to_tsvector('pt_unaccent', coalesce(translate($2, '-', ' '), '')), 'C')
        || setweight(to_tsvector('raw_unaccent', coalesce(translate($1, '-', ' '), '')), 'A')
        || setweight(to_tsvector('raw_unaccent', coalesce(translate($2, '-', ' '), '')), 'C')
$$;

-- nome A / marca(grife) B / descrição C / tamanho+cor D
CREATE OR REPLACE FUNCTION product_search_vec(text, text, text, text, text) RETURNS tsvector
    LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT setweight(to_tsvector('pt_unaccent', coalesce(translate($1, '-', ' '), '')), 'A')
        || setweight(to_tsvector('pt_unaccent', coalesce(translate($4, '-', ' '), '')), 'B')
        || setweight(to_tsvector('pt_unaccent', coalesce(translate($2, '-', ' '), '')), 'C')
        || setweight(to_tsvector('pt_unaccent', coalesce($3, '') || ' ' || coalesce($5, '')), 'D')
        || setweight(to_tsvector('raw_unaccent', coalesce(translate($1, '-', ' '), '')), 'A')
        || setweight(to_tsvector('raw_unaccent', coalesce(translate($4, '-', ' '), '')), 'B')
        || setweight(to_tsvector('raw_unaccent', coalesce(translate($2, '-', ' '), '')), 'C')
        || setweight(to_tsvector('raw_unaccent', coalesce($3, '') || ' ' || coalesce($5, '')), 'D')
$$;

-- rental_items.legacy_id é INT: vetor sobre o cast dá prefixo "42:*" = '425'
CREATE OR REPLACE FUNCTION legacy_id_search_vec(integer) RETURNS tsvector
    LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT to_tsvector('pt_unaccent', coalesce($1::text, ''))
$$;

-- retail não tem legacy_id; sku é o identificador (A) + details (C)
CREATE OR REPLACE FUNCTION retail_search_vec(text, text) RETURNS tsvector
    LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT setweight(to_tsvector('pt_unaccent', coalesce(translate($1, '-', ' '), '')), 'A')
        || setweight(to_tsvector('pt_unaccent', coalesce(translate($2, '-', ' '), '')), 'C')
        || setweight(to_tsvector('raw_unaccent', coalesce(translate($1, '-', ' '), '')), 'A')
        || setweight(to_tsvector('raw_unaccent', coalesce(translate($2, '-', ' '), '')), 'C')
$$;

-- customer_name é snapshot denormalizado na própria tabela (A) + legacy_id (B)
CREATE OR REPLACE FUNCTION contract_search_vec(text, text) RETURNS tsvector
    LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT setweight(to_tsvector('pt_unaccent', coalesce(translate($2, '-', ' '), '')), 'A')
        || setweight(to_tsvector('pt_unaccent', coalesce(translate($1, '-', ' '), '')), 'B')
        || setweight(to_tsvector('raw_unaccent', coalesce(translate($2, '-', ' '), '')), 'A')
        || setweight(to_tsvector('raw_unaccent', coalesce(translate($1, '-', ' '), '')), 'B')
$$;

CREATE INDEX IF NOT EXISTS idx_people_fts
    ON people USING GIN ((customer_search_vec(name, email)));

CREATE INDEX IF NOT EXISTS idx_products_fts
    ON products USING GIN ((product_search_vec(name, description, size, brand, color)));

CREATE INDEX IF NOT EXISTS idx_categories_fts ON categories USING GIN ((
    to_tsvector('pt_unaccent', coalesce(display_name, ''))));

CREATE INDEX IF NOT EXISTS idx_rental_items_fts
    ON rental_items USING GIN ((legacy_id_search_vec(legacy_id)));

CREATE INDEX IF NOT EXISTS idx_retail_products_fts
    ON retail_products USING GIN ((retail_search_vec(sku, details)));

CREATE INDEX IF NOT EXISTS idx_contracts_fts
    ON rental_contracts USING GIN ((contract_search_vec(legacy_id, customer_name)));
