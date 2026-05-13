-- V18__add-return-granular-fields.sql
-- Suporte a devolução granular por item e acessório (Sistema de Devolução v2)

ALTER TABLE rental_contract_items
    ADD COLUMN returned          BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN returned_at       TIMESTAMPTZ,
    ADD COLUMN returned_by_name  VARCHAR(255);

ALTER TABLE rental_contract_item_meta
    ADD COLUMN returned    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN returned_at TIMESTAMPTZ;
