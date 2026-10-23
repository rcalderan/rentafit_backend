-- V42__add_finance_movement_indexes.sql
-- Índices compostos para consultas de movimentação financeira (diária, mensal e anual)
-- em rental_payments e sales_payments.

CREATE INDEX IF NOT EXISTS idx_rental_payments_date_status
    ON rental_payments(payment_date, status);

CREATE INDEX IF NOT EXISTS idx_sales_payments_date_status
    ON sales_payments(payment_date, status);
