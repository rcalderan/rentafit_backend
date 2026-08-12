-- V24__fiscal_document_service_and_cancellation_fields.sql
-- Adiciona campos de descrição de serviço e cancelamento à tabela fiscal_documents.

ALTER TABLE fiscal_documents
    ADD COLUMN service_description VARCHAR(500),
    ADD COLUMN cancel_reason VARCHAR(500),
    ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN cancel_protocol VARCHAR(50);

COMMENT ON COLUMN fiscal_documents.service_description IS 'Descrição do serviço prestado (NFS-e)';
COMMENT ON COLUMN fiscal_documents.cancel_reason IS 'Motivo do cancelamento fiscal';
COMMENT ON COLUMN fiscal_documents.cancelled_at IS 'Data/hora do cancelamento fiscal';
COMMENT ON COLUMN fiscal_documents.cancel_protocol IS 'Protocolo de autorização do cancelamento';
