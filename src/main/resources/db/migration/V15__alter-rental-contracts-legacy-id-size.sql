-- V15__alter-rental-contracts-legacy-id-size.sql
-- Amplia legacy_id para suportar o novo formato YYYYMMDD-N (ex: 20260329-1)
-- IDs legados continuam compatíveis (eram sequenciais simples como "1", "2", "3")

ALTER TABLE rental_contracts ALTER COLUMN legacy_id TYPE VARCHAR(20);

