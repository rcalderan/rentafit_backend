-- V8__create-table-stock.sql
-- Create stock table for inventory control

CREATE TABLE stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL UNIQUE,
    quantity_available INTEGER NOT NULL DEFAULT 0,
    quantity_reserved INTEGER NOT NULL DEFAULT 0,
    quantity_total INTEGER NOT NULL DEFAULT 0,
    min_stock_level INTEGER NOT NULL DEFAULT 5,
    location VARCHAR(100),
    last_movement_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT chk_stock_quantities CHECK (
        quantity_available >= 0 AND
        quantity_reserved >= 0 AND
        quantity_total >= 0 AND
        quantity_total = quantity_available + quantity_reserved
    ),
    CONSTRAINT chk_min_stock_level CHECK (min_stock_level >= 0)
);

CREATE INDEX idx_stock_product_id ON stock(product_id);
CREATE INDEX idx_stock_location ON stock(location) WHERE location IS NOT NULL;
CREATE INDEX idx_stock_low_stock ON stock(quantity_available, min_stock_level)
    WHERE quantity_available < min_stock_level;
CREATE INDEX idx_stock_last_movement ON stock(last_movement_date) WHERE last_movement_date IS NOT NULL;

COMMENT ON TABLE stock IS 'Inventory control for RetailProduct and SALE-type Accessories';
COMMENT ON COLUMN stock.quantity_available IS 'Available for sale';
COMMENT ON COLUMN stock.quantity_reserved IS 'Reserved in pending sales';
