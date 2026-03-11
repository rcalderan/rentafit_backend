-- V13__create-table-rental-contract-items.sql
-- Itens e metadados dos contratos de locação

CREATE TABLE rental_contract_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    contract_id UUID NOT NULL,

    -- ON DELETE SET NULL: preserva o histórico do contrato mesmo se o item for removido do catálogo
    rental_item_id UUID,

    -- Snapshot do produto no momento da criação
    legacy_product_code VARCHAR(100),
    description VARCHAR(500) NOT NULL,
    value DECIMAL(10, 2) NOT NULL,

    is_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    attendant_employee_id UUID,

    CONSTRAINT fk_rental_items_contract
        FOREIGN KEY (contract_id) REFERENCES rental_contracts(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_items_product
        FOREIGN KEY (rental_item_id) REFERENCES rental_items(id) ON DELETE SET NULL,
    CONSTRAINT fk_rental_items_attendant
        FOREIGN KEY (attendant_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT chk_rental_item_value
        CHECK (value > 0)
);

CREATE TABLE rental_contract_item_meta (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    contract_item_id UUID NOT NULL,

    type VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,

    -- ON DELETE SET NULL: metadado textual permanece mesmo se o acessório for removido
    accessory_id UUID,

    CONSTRAINT fk_item_meta_contract_item
        FOREIGN KEY (contract_item_id) REFERENCES rental_contract_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_meta_accessory
        FOREIGN KEY (accessory_id) REFERENCES accessories(id) ON DELETE SET NULL,
    CONSTRAINT chk_item_meta_type
        CHECK (type IN ('ACESSORIO', 'OBSERVACAO'))
);

-- Índices
CREATE INDEX idx_rental_contract_items_contract_id    ON rental_contract_items(contract_id);
CREATE INDEX idx_rental_contract_items_rental_item_id ON rental_contract_items(rental_item_id) WHERE rental_item_id IS NOT NULL;
CREATE INDEX idx_rental_item_meta_contract_item_id    ON rental_contract_item_meta(contract_item_id);
CREATE INDEX idx_rental_item_meta_accessory_id        ON rental_contract_item_meta(accessory_id) WHERE accessory_id IS NOT NULL;

COMMENT ON TABLE  rental_contract_items                IS 'Itens de locação vinculados a um contrato';
COMMENT ON COLUMN rental_contract_items.rental_item_id IS 'FK para rental_items — SET NULL se item removido do catálogo';
COMMENT ON COLUMN rental_contract_items.description    IS 'Snapshot da descrição do produto na criação';
COMMENT ON TABLE  rental_contract_item_meta            IS 'Metadados dos itens: acessórios catalogados ou observações textuais';
COMMENT ON COLUMN rental_contract_item_meta.accessory_id IS 'FK opcional — quando preenchido, aciona controle de estoque via StockService';

