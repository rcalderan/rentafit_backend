-- V20: Criação das tabelas do módulo Sales (vendas de varejo)

-- Adicionar warranty_days ao retail_products
ALTER TABLE retail_products ADD COLUMN IF NOT EXISTS warranty_days INTEGER;

-- Tabela principal de pedidos de venda
CREATE TABLE sales_orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legacy_id       VARCHAR(30) UNIQUE,
    customer_id     UUID,
    customer_name   VARCHAR(255),
    customer_document VARCHAR(20),
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes           TEXT DEFAULT '',
    discount_value  NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    cancellation_reason TEXT,
    invoice_status  VARCHAR(30) NOT NULL DEFAULT 'NONE',
    invoice_id      VARCHAR(255),
    created_by_employee_id UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_sales_orders_status ON sales_orders(status);
CREATE INDEX idx_sales_orders_customer ON sales_orders(customer_id);
CREATE INDEX idx_sales_orders_legacy_id ON sales_orders(legacy_id);
CREATE INDEX idx_sales_orders_created_at ON sales_orders(created_at);

-- Tabela de itens do pedido de venda
CREATE TABLE sales_order_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sales_order_id          UUID NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    retail_product_id       UUID NOT NULL,
    sku                     VARCHAR(100) NOT NULL,
    description             VARCHAR(500) NOT NULL,
    unit_price              NUMERIC(10,2) NOT NULL,
    quantity                INTEGER NOT NULL DEFAULT 1,
    discount_value          NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    item_status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attendant_employee_id   UUID,
    needs_tailoring         BOOLEAN NOT NULL DEFAULT false,
    tailoring_notes         TEXT,
    delivered_at            TIMESTAMPTZ,
    delivered_by_employee_id UUID
);

CREATE INDEX idx_sales_items_order ON sales_order_items(sales_order_id);
CREATE INDEX idx_sales_items_product ON sales_order_items(retail_product_id);

-- Tabela de pagamentos do pedido de venda
CREATE TABLE sales_payments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sales_order_id          UUID NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    installment_number      INTEGER NOT NULL,
    payment_date            DATE NOT NULL,
    payment_method          VARCHAR(20) NOT NULL,
    value                   NUMERIC(10,2) NOT NULL,
    installments            INTEGER NOT NULL DEFAULT 1,
    processed_by_employee_id UUID,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_sales_payments_order ON sales_payments(sales_order_id);
