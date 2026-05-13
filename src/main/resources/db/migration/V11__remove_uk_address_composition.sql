-- V11__remove_uk_address_composition.sql
-- Remove unique constraint on address composition to allow multiple customers at the same address.
-- Deduplication is now handled at the application layer (AddressService.findOrCreateByAddress).

ALTER TABLE addresses DROP CONSTRAINT IF EXISTS uk_address_composition;

