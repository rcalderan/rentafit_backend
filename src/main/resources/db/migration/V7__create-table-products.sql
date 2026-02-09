-- V7__create-table-products.sql
-- Create products tables with JOINED inheritance strategy

-- Base table for all products
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    size VARCHAR(50),
    color VARCHAR(50),
    brand VARCHAR(100),
    value DECIMAL(10,2) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Table for Rental Items (extends products)
CREATE TABLE rental_items (
    id UUID PRIMARY KEY,
    legacy_id VARCHAR(50) UNIQUE,
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    condition VARCHAR(20),
    last_rental_date TIMESTAMP,
    rental_count INTEGER DEFAULT 0,
    maintenance_due_date DATE,

    CONSTRAINT fk_rental_items_product FOREIGN KEY (id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT chk_rental_status CHECK (status IN ('AVAILABLE', 'RENTED', 'MAINTENANCE', 'RESERVED', 'DAMAGED', 'RETIRED', 'INACTIVE')),
    CONSTRAINT chk_rental_condition CHECK (condition IN ('NEW', 'EXCELLENT', 'GOOD', 'FAIR', 'POOR'))
);

-- Table for Retail Products (extends products)
CREATE TABLE retail_products (
    id UUID PRIMARY KEY,
    details TEXT,
    sku VARCHAR(100) UNIQUE,

    CONSTRAINT fk_retail_products_product FOREIGN KEY (id) REFERENCES products(id) ON DELETE CASCADE
);

-- Table for Accessories (extends products)
CREATE TABLE accessories (
    id UUID PRIMARY KEY,
    legacy_id VARCHAR(50) UNIQUE,
    compatible_with VARCHAR(500),

    CONSTRAINT fk_accessories_product FOREIGN KEY (id) REFERENCES products(id) ON DELETE CASCADE
);

-- Indexes for base table
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_created_at ON products(created_at);

-- Indexes for rental_items
CREATE INDEX idx_rental_items_legacy_id ON rental_items(legacy_id) WHERE legacy_id IS NOT NULL;
CREATE INDEX idx_rental_items_status ON rental_items(status);
CREATE INDEX idx_rental_items_maintenance_due ON rental_items(maintenance_due_date) WHERE maintenance_due_date IS NOT NULL;

-- Indexes for retail_products
CREATE INDEX idx_retail_products_sku ON retail_products(sku) WHERE sku IS NOT NULL;

-- Indexes for accessories
CREATE INDEX idx_accessories_legacy_id ON accessories(legacy_id) WHERE legacy_id IS NOT NULL;
CREATE INDEX idx_accessories_compatible ON accessories(compatible_with) WHERE compatible_with IS NOT NULL;

-- Comments
COMMENT ON TABLE products IS 'Base table for all products (JOINED inheritance)';
COMMENT ON TABLE rental_items IS 'Items for rental - individual tracked items';
COMMENT ON TABLE retail_products IS 'Products for retail sale - stock controlled';
COMMENT ON TABLE accessories IS 'Complementary accessories - can be rental or sale';

COMMENT ON COLUMN rental_items.legacy_id IS 'Legacy system ID (String format RI-XXXXXX)';
COMMENT ON COLUMN rental_items.status IS 'Current status: AVAILABLE, RENTED, MAINTENANCE, RESERVED, DAMAGED, RETIRED, INACTIVE';
COMMENT ON COLUMN rental_items.condition IS 'Physical condition: NEW, EXCELLENT, GOOD, FAIR, POOR';
COMMENT ON COLUMN retail_products.sku IS 'SKU for inventory control';
COMMENT ON COLUMN accessories.legacy_id IS 'Legacy system ID for rental accessories (AC-XXXXXX)';
COMMENT ON COLUMN accessories.compatible_with IS 'Compatible product categories (comma separated)';
