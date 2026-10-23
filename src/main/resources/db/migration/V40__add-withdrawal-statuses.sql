-- V40__add-withdrawal-statuses.sql
-- Fluxo de desistência: contrato CANCELLED e parcela REFUNDED.

-- ── rental_contracts: incluir CANCELLED ──────────────────────────────────────
ALTER TABLE rental_contracts DROP CONSTRAINT chk_rental_contract_status;
ALTER TABLE rental_contracts ADD CONSTRAINT chk_rental_contract_status
    CHECK (status IN ('DRAFT', 'SIGNED', 'FINALIZED', 'REVISION', 'SUPERSEDED', 'CLOSED', 'CANCELLED'));

-- ── rental_payments: incluir REFUNDED ────────────────────────────────────────
ALTER TABLE rental_payments DROP CONSTRAINT chk_rental_payment_status;
ALTER TABLE rental_payments ADD CONSTRAINT chk_rental_payment_status
    CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'MULTA', 'REFUNDED'));
