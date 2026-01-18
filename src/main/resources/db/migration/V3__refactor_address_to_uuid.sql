-- Migration to refactor addresses to use UUID PK and support manual entries

-- 1. Add id and is_manual to addresses
ALTER TABLE addresses ADD COLUMN id UUID;
-- Using gen_random_uuid() which is available in PostgreSQL 13+ or via pgcrypto
UPDATE addresses SET id = gen_random_uuid() WHERE id IS NULL;
ALTER TABLE addresses ALTER COLUMN id SET NOT NULL;
ALTER TABLE addresses ADD COLUMN is_manual BOOLEAN DEFAULT FALSE;

-- 2. Modify person_address_details to link by address_id (UUID)
ALTER TABLE person_address_details ADD COLUMN address_id UUID;

-- Update address_id based on zip_code
UPDATE person_address_details pad
SET address_id = a.id
FROM addresses a
WHERE pad.zip_code = a.zip_code;

ALTER TABLE person_address_details ALTER COLUMN address_id SET NOT NULL;

-- 3. Drop old foreign key and column from person_address_details
ALTER TABLE person_address_details DROP CONSTRAINT fk_person_address_zipcode;
ALTER TABLE person_address_details DROP COLUMN zip_code;

-- 4. Set new PK for addresses
ALTER TABLE addresses DROP CONSTRAINT addresses_pkey;
ALTER TABLE addresses ADD PRIMARY KEY (id);

-- 5. Make zip_code nullable and add unique constraint
ALTER TABLE addresses ALTER COLUMN zip_code DROP NOT NULL;
-- Note: A unique constraint on multiple columns where some can be null
-- might still allow multiple (null, street, city, state) in some DBs.
-- However, for our purposes it's the standard way.
ALTER TABLE addresses ADD CONSTRAINT uk_address_composition UNIQUE (zip_code, street, city, state);

-- 6. Add foreign key back to person_address_details
ALTER TABLE person_address_details
ADD CONSTRAINT fk_person_address_address_id
FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE CASCADE;

-- 7. Update person_address_history
ALTER TABLE person_address_history ALTER COLUMN zip_code DROP NOT NULL;
ALTER TABLE person_address_history ADD COLUMN is_manual BOOLEAN DEFAULT FALSE;
