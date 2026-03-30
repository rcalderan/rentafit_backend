-- V16__add-contract-revision-support.sql
-- Suporte a revisões de contrato: campo parent_contract_id e novos status REVISION / SUPERSEDED

-- Adiciona referência ao contrato-pai (o que foi substituído pela revisão)
ALTER TABLE rental_contracts ADD COLUMN parent_contract_id UUID REFERENCES rental_contracts(id) ON DELETE SET NULL;

-- Atualiza CHECK de status para incluir REVISION e SUPERSEDED
ALTER TABLE rental_contracts DROP CONSTRAINT chk_rental_contract_status;
ALTER TABLE rental_contracts ADD CONSTRAINT chk_rental_contract_status
    CHECK (status IN ('DRAFT', 'SIGNED', 'FINALIZED', 'REVISION', 'SUPERSEDED'));

-- Índice para busca por contrato-pai
CREATE INDEX idx_rental_contracts_parent ON rental_contracts(parent_contract_id) WHERE parent_contract_id IS NOT NULL;

COMMENT ON COLUMN rental_contracts.parent_contract_id IS 'UUID do contrato substituído por esta revisão (null = contrato original)';

