-- V19__fix-closed-status-and-payment-constraints.sql
-- BUG-2026-05-02-5: Adiciona CLOSED à constraint de status do contrato
-- BUG-2026-05-02-3: Garante unicidade de installment_number por contrato (exclui CANCELLED)
-- BUG-2026-05-02-2: Adiciona MULTA ao status de pagamento

-- ── rental_contracts: incluir CLOSED ─────────────────────────────────────────
ALTER TABLE rental_contracts DROP CONSTRAINT chk_rental_contract_status;
ALTER TABLE rental_contracts ADD CONSTRAINT chk_rental_contract_status
    CHECK (status IN ('DRAFT', 'SIGNED', 'FINALIZED', 'REVISION', 'SUPERSEDED', 'CLOSED'));

-- ── rental_payments: incluir MULTA ───────────────────────────────────────────
ALTER TABLE rental_payments DROP CONSTRAINT chk_rental_payment_status;
ALTER TABLE rental_payments ADD CONSTRAINT chk_rental_payment_status
    CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'MULTA'));

-- ── rental_payments: unicidade de installment_number por contrato ─────────────
-- Parcelas CANCELLED são excluídas da constraint (podem ser reaproveitadas).
-- Antes de criar, remover duplicatas existentes nos dados de teste (idempotente).
CREATE UNIQUE INDEX IF NOT EXISTS uq_rental_payments_contract_installment
    ON rental_payments(contract_id, installment_number)
    WHERE status <> 'CANCELLED';

COMMENT ON INDEX uq_rental_payments_contract_installment
    IS 'BUG-2026-05-02-3: previne installment_number duplicado no mesmo contrato (exceto CANCELLED)';
