-- V17__add-replaced-by-contract-id.sql
-- Vínculo bidirecional entre contrato superseded e o vigente que o substituiu

ALTER TABLE rental_contracts ADD COLUMN replaced_by_contract_id UUID REFERENCES rental_contracts(id) ON DELETE SET NULL;

COMMENT ON COLUMN rental_contracts.replaced_by_contract_id IS 'UUID do contrato vigente que substituiu este (preenchido quando status = SUPERSEDED)';

