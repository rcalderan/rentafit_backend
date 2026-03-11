-- V12__create-table-rental-contracts.sql
-- Tabela principal de contratos de locação

CREATE TABLE rental_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    legacy_id INTEGER UNIQUE,

    contract_type INTEGER NOT NULL DEFAULT 0,

    -- Snapshot imutável do cliente (preservado mesmo se o cadastro mudar)
    customer_name VARCHAR(255) NOT NULL,
    customer_document VARCHAR(255),

    -- Referências cruzadas (UUID puro para desacoplamento de componentes)
    customer_id UUID NOT NULL,
    created_by_employee_id UUID,
    returned_by_employee_id UUID,

    -- Datas do contrato
    pickup_date DATE NOT NULL,
    event_date DATE NOT NULL,
    return_date DATE NOT NULL,
    actual_return_date DATE,

    -- Estado
    is_returned BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes TEXT NOT NULL DEFAULT '',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FKs (componente People)
    CONSTRAINT fk_rental_contracts_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_contracts_created_by
        FOREIGN KEY (created_by_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT fk_rental_contracts_returned_by
        FOREIGN KEY (returned_by_employee_id) REFERENCES employees(id) ON DELETE SET NULL,

    CONSTRAINT chk_rental_contract_status
        CHECK (status IN ('DRAFT', 'SIGNED', 'FINALIZED')),
    CONSTRAINT chk_rental_contract_dates
        CHECK (pickup_date <= event_date AND event_date <= return_date)
);

-- Índices para queries de listagem e conflito
CREATE INDEX idx_rental_contracts_customer_id   ON rental_contracts(customer_id);
CREATE INDEX idx_rental_contracts_status        ON rental_contracts(status);
CREATE INDEX idx_rental_contracts_event_date    ON rental_contracts(event_date);
CREATE INDEX idx_rental_contracts_legacy_id     ON rental_contracts(legacy_id) WHERE legacy_id IS NOT NULL;

COMMENT ON TABLE  rental_contracts                  IS 'Contratos de locação (proposta → assinado → finalizado)';
COMMENT ON COLUMN rental_contracts.customer_name    IS 'Snapshot do nome do cliente na criação — imutável';
COMMENT ON COLUMN rental_contracts.customer_document IS 'Snapshot do documento do cliente na criação — imutável';
COMMENT ON COLUMN rental_contracts.event_date       IS 'Data do evento (uso da roupa). Indexada para checagem de conflito';
COMMENT ON COLUMN rental_contracts.status           IS 'DRAFT=proposta, SIGNED=assinado, FINALIZED=contrato fechado';

