-- V26__add_fiscal_document_customer_snapshot.sql
-- Adiciona snapshot do cliente ao documento fiscal para casos em que
-- o Customer não existe no cadastro ou não é encontrado por documento.

ALTER TABLE fiscal_documents
    ADD COLUMN customer_name VARCHAR(255),
    ADD COLUMN customer_email VARCHAR(255);

COMMENT ON COLUMN fiscal_documents.customer_name IS 'Nome do cliente (snapshot)';
COMMENT ON COLUMN fiscal_documents.customer_email IS 'E-mail do cliente (snapshot)';
