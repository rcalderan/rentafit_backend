-- V5__remove_phone_from_pk.sql
-- Migration to remove phone from the primary key of customer_phones
-- and replace it with a surrogate UUID primary key.

-- 1. Drop existing composite primary key
ALTER TABLE customer_phones DROP CONSTRAINT IF EXISTS customer_phones_pkey;

-- 2. Add a new surrogate primary key column
-- Note: gen_random_uuid() is available in PostgreSQL 13+
ALTER TABLE customer_phones ADD COLUMN id UUID DEFAULT gen_random_uuid();

-- 3. Set the new PK
ALTER TABLE customer_phones ADD PRIMARY KEY (id);
