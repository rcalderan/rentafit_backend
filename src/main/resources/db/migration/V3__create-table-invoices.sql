CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    access_key VARCHAR(50) UNIQUE NOT NULL,
    invoice_number BIGINT NOT NULL,
    customer_id UUID NOT NULL,
    issue_date TIMESTAMP WITH TIME ZONE NOT NULL,
    service_value DECIMAL(19, 4) NOT NULL,
    ibs_rate DECIMAL(19, 4),
    ibs_value DECIMAL(19, 4),
    cbs_rate DECIMAL(19, 4),
    cbs_value DECIMAL(19, 4),
    isqn_rate DECIMAL(19, 4),
    isqn_value DECIMAL(19, 4),
    total_tax_value DECIMAL(19, 4),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE INDEX idx_invoices_access_key ON invoices(access_key);
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
