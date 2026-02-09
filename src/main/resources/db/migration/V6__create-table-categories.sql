-- V6__create-table-categories.sql
-- Create categories table

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    product_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_product_type CHECK (product_type IN ('RENTAL', 'RETAIL', 'ACCESSORY'))
);

CREATE INDEX idx_categories_product_type ON categories(product_type);
CREATE INDEX idx_categories_active ON categories(active);

COMMENT ON TABLE categories IS 'Product categories: RENTAL, RETAIL, ACCESSORY';
COMMENT ON COLUMN categories.product_type IS 'Type of product: RENTAL (items), RETAIL (stock), ACCESSORY (both)';
