-- V10__insert-product-categories.sql
-- Insert pre-configured product categories

-- RENTAL Categories
INSERT INTO categories (id, name, display_name, description, product_type, active, created_at, updated_at) VALUES
    (gen_random_uuid(), 'FESTA_RENTAL', 'Vestidos de Festa', 'Vestidos para eventos festivos', 'RENTAL', true, NOW(), NOW()),
    (gen_random_uuid(), 'CASUAL_RENTAL', 'Roupas Casuais', 'Roupas para uso casual', 'RENTAL', true, NOW(), NOW()),
    (gen_random_uuid(), 'SOCIAL_RENTAL', 'Roupas Sociais', 'Roupas para eventos sociais e profissionais', 'RENTAL', true, NOW(), NOW()),
    (gen_random_uuid(), 'NOIVAS_RENTAL', 'Vestidos de Noiva', 'Vestidos para casamento', 'RENTAL', true, NOW(), NOW()),
    (gen_random_uuid(), 'TERNOS_RENTAL', 'Ternos', 'Ternos para aluguel', 'RENTAL', true, NOW(), NOW());

-- RETAIL Categories
INSERT INTO categories (id, name, display_name, description, product_type, active, created_at, updated_at) VALUES
    (gen_random_uuid(), 'VESTIDOS_RETAIL', 'Vestidos para Venda', 'Vestidos para venda no varejo', 'RETAIL', true, NOW(), NOW()),
    (gen_random_uuid(), 'TERNOS_RETAIL', 'Ternos para Venda', 'Ternos para venda no varejo', 'RETAIL', true, NOW(), NOW()),
    (gen_random_uuid(), 'SAPATOS_RETAIL', 'Sapatos para Venda', 'Sapatos para venda', 'RETAIL', true, NOW(), NOW()),
    (gen_random_uuid(), 'GRAVATAS_RETAIL', 'Gravatas para Venda', 'Gravatas para venda', 'RETAIL', true, NOW(), NOW()),
    (gen_random_uuid(), 'CINTOS_RETAIL', 'Cintos para Venda', 'Cintos para venda', 'RETAIL', true, NOW(), NOW());

-- ACCESSORY Categories (RENTAL - Unique Items)
INSERT INTO categories (id, name, display_name, description, product_type, active, created_at, updated_at) VALUES
    (gen_random_uuid(), 'BOLSAS_RENTAL', 'Bolsas para Aluguel', 'Bolsas de alto valor para aluguel', 'ACCESSORY', true, NOW(), NOW()),
    (gen_random_uuid(), 'JOIAS_RENTAL', 'Joias para Aluguel', 'Joias complementares para aluguel', 'ACCESSORY', true, NOW(), NOW()),
    (gen_random_uuid(), 'SAPATOS_RENTAL', 'Sapatos para Aluguel', 'Sapatos complementares para aluguel', 'ACCESSORY', true, NOW(), NOW());

-- ACCESSORY Categories (Stock-based)
INSERT INTO categories (id, name, display_name, description, product_type, active, created_at, updated_at) VALUES
    (gen_random_uuid(), 'GRAVATAS_ACCESSORY', 'Gravatas Acessório', 'Gravatas para complementar', 'ACCESSORY', true, NOW(), NOW()),
    (gen_random_uuid(), 'LENCOS_ACCESSORY', 'Lenços Acessório', 'Lenços e acessórios complementares', 'ACCESSORY', true, NOW(), NOW());
