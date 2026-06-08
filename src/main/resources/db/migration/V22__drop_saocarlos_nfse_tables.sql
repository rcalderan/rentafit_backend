-- V22__drop_saocarlos_nfse_tables.sql
-- Remove tabelas obsoletas do sistema GINFES São Carlos (substituído por NFS-e Nacional)
-- Ordem: tabelas filhas primeiro (referências FK), depois tabelas pais

-- Remove tabela de NFS-e geradas (referencia rps e lote)
DROP TABLE IF EXISTS saocarlos_nfse CASCADE;

-- Remove tabela de RPS (referencia lote)
DROP TABLE IF EXISTS saocarlos_rps CASCADE;

-- Remove tabela de lotes de RPS
DROP TABLE IF EXISTS saocarlos_lote_rps CASCADE;
