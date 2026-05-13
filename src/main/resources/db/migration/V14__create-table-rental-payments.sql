-- V14__create-table-rental-payments.sql
-- Parcelas de pagamento dos contratos de locação

CREATE TABLE rental_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    contract_id UUID NOT NULL,

    installment_number INTEGER NOT NULL,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    value DECIMAL(10, 2) NOT NULL,

    -- Número de vezes (para cartão de crédito). Default 1 para pagamentos à vista.
    installments INTEGER NOT NULL DEFAULT 1,

    processed_by_employee_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    CONSTRAINT fk_rental_payments_contract
        FOREIGN KEY (contract_id) REFERENCES rental_contracts(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_payments_employee
        FOREIGN KEY (processed_by_employee_id) REFERENCES employees(id) ON DELETE SET NULL,

    CONSTRAINT chk_rental_payment_installment_number
        CHECK (installment_number BETWEEN 1 AND 24),
    CONSTRAINT chk_rental_payment_value
        CHECK (value > 0),
    CONSTRAINT chk_rental_payment_installments
        CHECK (installments BETWEEN 1 AND 24),
    CONSTRAINT chk_rental_payment_status
        CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
    CONSTRAINT chk_rental_payment_method
        CHECK (payment_method IN ('CASH', 'PIX', 'CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER'))
);

-- Índices
CREATE INDEX idx_rental_payments_contract_id ON rental_payments(contract_id);
CREATE INDEX idx_rental_payments_status      ON rental_payments(status);
CREATE INDEX idx_rental_payments_date        ON rental_payments(payment_date);

COMMENT ON TABLE  rental_payments                    IS 'Parcelas de pagamento de contratos de locação';
COMMENT ON COLUMN rental_payments.installment_number IS 'Número da parcela (1-24)';
COMMENT ON COLUMN rental_payments.installments       IS 'Número de vezes no cartão (1=à vista)';
COMMENT ON COLUMN rental_payments.status             IS 'PENDING=pendente, PAID=pago, CANCELLED=cancelado';

