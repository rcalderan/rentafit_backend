-- V23__create_fiscal_documents.sql
-- Cria tabela fiscal_documents (substitui invoices) e migra dados existentes

CREATE TABLE fiscal_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    model INTEGER,
    series VARCHAR(5),
    number BIGINT,
    access_key VARCHAR(60) UNIQUE,
    protocol VARCHAR(50),
    authorization_date TIMESTAMP WITH TIME ZONE,
    customer_id UUID REFERENCES customers(id),
    issue_date TIMESTAMP WITH TIME ZONE NOT NULL,
    total_value DECIMAL(15,2) NOT NULL,
    ibs_rate DECIMAL(8,6),
    ibs_value DECIMAL(15,2),
    cbs_rate DECIMAL(8,6),
    cbs_value DECIMAL(15,2),
    isqn_rate DECIMAL(8,6),
    isqn_value DECIMAL(15,2),
    total_tax_value DECIMAL(15,2),
    origin VARCHAR(10),
    origin_id UUID,
    signed_xml TEXT,
    authorized_xml TEXT,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fiscal_documents_access_key ON fiscal_documents(access_key);
CREATE INDEX idx_fiscal_documents_type_origin_id ON fiscal_documents(type, origin_id);
CREATE INDEX idx_fiscal_documents_origin_origin_id ON fiscal_documents(origin, origin_id);
CREATE INDEX idx_fiscal_documents_customer ON fiscal_documents(customer_id);
CREATE INDEX idx_fiscal_documents_status ON fiscal_documents(status);

COMMENT ON TABLE fiscal_documents IS 'Documentos fiscais emitidos: NF-e (produtos) e NFS-e (serviços)';
COMMENT ON COLUMN fiscal_documents.type IS 'NFE ou NFSE';
COMMENT ON COLUMN fiscal_documents.status IS 'PENDING, SIGNED, TRANSMITTED, AUTHORIZED, REJECTED, CANCELLED';
COMMENT ON COLUMN fiscal_documents.model IS '55 = NF-e, 99 = NFS-e nacional';
COMMENT ON COLUMN fiscal_documents.origin IS 'SALES, RENTAL, MANUAL';

-- Migra dados da tabela invoices (se existir e tiver dados)
INSERT INTO fiscal_documents (
    id, type, status, access_key, customer_id, issue_date,
    total_value, ibs_rate, ibs_value, cbs_rate, cbs_value,
    isqn_rate, isqn_value, total_tax_value, origin, created_at
)
SELECT
    id,
    'NFSE' AS type,
    CASE status
        WHEN 'AUTHORIZED' THEN 'AUTHORIZED'
        WHEN 'CANCELLED'  THEN 'CANCELLED'
        ELSE 'AUTHORIZED'
    END AS status,
    access_key,
    customer_id,
    issue_date,
    service_value AS total_value,
    ibs_rate, ibs_value, cbs_rate, cbs_value,
    isqn_rate, isqn_value, total_tax_value,
    'MANUAL' AS origin,
    created_at
FROM invoices
WHERE EXISTS (SELECT 1 FROM invoices LIMIT 1);

DROP TABLE IF EXISTS invoices;
