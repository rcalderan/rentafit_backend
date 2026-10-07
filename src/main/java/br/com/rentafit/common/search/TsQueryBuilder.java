package br.com.rentafit.common.search;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Converte texto livre do usuário em tsquery segura para os índices 'pt_unaccent'.
 *
 * <p>Só letras/dígitos viram tokens — operadores tsquery (&amp; | ! ( ) : *) e
 * pontuação são removidos antes de montar a query, evitando {@code syntax error
 * in tsquery}. A normalização de acentos/caixa acontece no banco pela config.</p>
 */
public final class TsQueryBuilder {

    private static final Pattern NON_TOKEN = Pattern.compile("[^\\p{L}\\p{N}]+");

    private TsQueryBuilder() {
    }

    /**
     * Ex.: ("maria  sil", PREFIX_LAST) → "maria & sil:*"; ("maria", PREFIX_ALL) → "maria:*".
     *
     * @return tsquery com tokens ANDed, ou null quando não há token válido —
     *         o caller deve cair para a listagem sem filtro.
     */
    public static String toTsQuery(String raw, SearchMode mode) {
        String[] tokens = tokenize(raw);
        if (tokens.length == 0) {
            return null;
        }
        int prefixFrom = mode == SearchMode.PREFIX_ALL ? 0 : tokens.length - 1;
        StringBuilder tsQuery = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                tsQuery.append(" & ");
            }
            tsQuery.append(tokens[i]);
            if (i >= prefixFrom) {
                tsQuery.append(":*");
            }
        }
        return tsQuery.toString();
    }

    /**
     * Tsquery para a config 'raw_unaccent': tokens crus ORed, sem prefixo — casa
     * só match exato por palavra inteira ("claudio" pega "Claudio", não "Claudiane").
     * Alimenta o boost de fts_rank_boosted, nunca o filtro WHERE.
     *
     * @return "maria | sil", ou null quando não há token válido.
     */
    public static String toExactQuery(String raw) {
        String[] tokens = tokenize(raw);
        return tokens.length == 0 ? null : String.join(" | ", tokens);
    }

    private static String[] tokenize(String raw) {
        if (raw == null) {
            return new String[0];
        }
        String cleaned = NON_TOKEN.matcher(raw).replaceAll(" ").trim();
        return cleaned.isEmpty() ? new String[0] : cleaned.split("\\s+");
    }

    /**
     * Substitui lista vazia de IDs por um UUID impossível — JPQL {@code IN :ids}
     * com coleção vazia não é portável; o sentinela nunca casa e mantém o plano BitmapOr.
     */
    public static List<UUID> idsOrNeverMatch(List<UUID> ids) {
        return ids.isEmpty() ? List.of(new UUID(0, 0)) : ids;
    }
}
