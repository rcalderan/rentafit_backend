-- V44__create_sectors_and_operational_tracking.sql
-- Tabela de setores (espaços físicos de atendimento/caixa/retirada) vinculados
-- a um estabelecimento (Matriz/Filial, identificado pelo CNPJ de 14 dígitos),
-- e rastreamento do setor nas operações de locação, venda e pagamento.

CREATE TABLE sectors (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issuer_cnpj  VARCHAR(14)  NOT NULL,
    branch_name  VARCHAR(255),
    name         VARCHAR(100) NOT NULL,
    code         VARCHAR(20)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_sectors_cnpj_name UNIQUE (issuer_cnpj, name),
    CONSTRAINT uq_sectors_cnpj_code UNIQUE (issuer_cnpj, code)
);

COMMENT ON TABLE  sectors              IS 'Setores físicos de atendimento/caixa/retirada por estabelecimento (CNPJ)';
COMMENT ON COLUMN sectors.issuer_cnpj  IS 'CNPJ (14 dígitos) da Matriz/Filial à qual o setor pertence — sincronizado com costume-rental-nfe';
COMMENT ON COLUMN sectors.code         IS 'Sigla curta do setor (ex.: BALCAO, DEPOSITO) — única por estabelecimento';
COMMENT ON COLUMN sectors.is_default   IS 'Setor padrão sugerido para terminais sem configuração';

-- Rastreamento de setor nas operações ─────────────────────────────────────────

ALTER TABLE rental_contracts
    ADD COLUMN sector_id        UUID,
    ADD COLUMN return_sector_id UUID,
    ADD CONSTRAINT fk_rental_contracts_sector        FOREIGN KEY (sector_id)        REFERENCES sectors(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_rental_contracts_return_sector FOREIGN KEY (return_sector_id) REFERENCES sectors(id) ON DELETE SET NULL;

ALTER TABLE rental_contract_items
    ADD COLUMN delivered_sector_id UUID,
    ADD COLUMN returned_sector_id  UUID,
    ADD CONSTRAINT fk_rci_delivered_sector FOREIGN KEY (delivered_sector_id) REFERENCES sectors(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_rci_returned_sector  FOREIGN KEY (returned_sector_id)  REFERENCES sectors(id) ON DELETE SET NULL;

ALTER TABLE rental_payments
    ADD COLUMN sector_id UUID,
    ADD CONSTRAINT fk_rental_payments_sector FOREIGN KEY (sector_id) REFERENCES sectors(id) ON DELETE SET NULL;

ALTER TABLE sales_orders
    ADD COLUMN sector_id UUID,
    ADD CONSTRAINT fk_sales_orders_sector FOREIGN KEY (sector_id) REFERENCES sectors(id) ON DELETE SET NULL;

ALTER TABLE sales_order_items
    ADD COLUMN delivered_sector_id UUID,
    ADD CONSTRAINT fk_soi_delivered_sector FOREIGN KEY (delivered_sector_id) REFERENCES sectors(id) ON DELETE SET NULL;

ALTER TABLE sales_payments
    ADD COLUMN sector_id UUID,
    ADD CONSTRAINT fk_sales_payments_sector FOREIGN KEY (sector_id) REFERENCES sectors(id) ON DELETE SET NULL;

-- Índices ────────────────────────────────────────────────────────────────────

CREATE INDEX idx_rental_contracts_sector_id ON rental_contracts(sector_id);
CREATE INDEX idx_rental_payments_sector_id  ON rental_payments(sector_id);
CREATE INDEX idx_sales_orders_sector_id     ON sales_orders(sector_id);
CREATE INDEX idx_sales_payments_sector_id   ON sales_payments(sector_id);

COMMENT ON COLUMN rental_contracts.sector_id        IS 'Setor do terminal onde o contrato foi aberto — define onde a peça é retirada';
COMMENT ON COLUMN rental_contracts.return_sector_id IS 'Setor do terminal onde a devolução foi concluída';
COMMENT ON COLUMN rental_payments.sector_id         IS 'Setor do terminal onde a parcela foi recebida (caixa do dia)';
COMMENT ON COLUMN sales_orders.sector_id            IS 'Setor do terminal onde a venda foi aberta';
COMMENT ON COLUMN sales_payments.sector_id          IS 'Setor do terminal onde a parcela foi recebida (caixa do dia)';
