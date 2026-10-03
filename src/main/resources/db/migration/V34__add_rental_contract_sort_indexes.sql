-- V34__add_rental_contract_sort_indexes.sql
-- Índices para os sorts usados pelo frontend em GET /api/v1/rental/contracts
-- (createdAt,desc no dashboard e returnDate,asc em pending-returns).
-- Sem eles, cada listagem faz seq scan + sort de ~31k linhas.

CREATE INDEX idx_rental_contracts_created_at  ON rental_contracts(created_at);
CREATE INDEX idx_rental_contracts_return_date ON rental_contracts(return_date);
