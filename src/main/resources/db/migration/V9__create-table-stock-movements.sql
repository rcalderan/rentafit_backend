-- V9__create-table-stock-movements.sql
-- Create stock movements audit table

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    movement_date TIMESTAMP NOT NULL,
    user_id UUID NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_movement_stock FOREIGN KEY (stock_id) REFERENCES stock(id) ON DELETE CASCADE,
    CONSTRAINT chk_movement_type CHECK (type IN ('ENTRADA', 'SAIDA', 'RESERVA', 'LIBERACAO', 'AJUSTE', 'PERDA')),
    CONSTRAINT chk_movement_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_stock_movements_stock_id ON stock_movements(stock_id);
CREATE INDEX idx_stock_movements_date ON stock_movements(movement_date DESC);
CREATE INDEX idx_stock_movements_type ON stock_movements(type);
CREATE INDEX idx_stock_movements_user_id ON stock_movements(user_id);
CREATE INDEX idx_stock_movements_created_at ON stock_movements(created_at DESC);
CREATE INDEX idx_stock_movements_stock_date ON stock_movements(stock_id, movement_date DESC);

COMMENT ON TABLE stock_movements IS 'Immutable audit log of stock movements';
COMMENT ON COLUMN stock_movements.type IS 'ENTRADA, SAIDA, RESERVA, LIBERACAO, AJUSTE, PERDA';
