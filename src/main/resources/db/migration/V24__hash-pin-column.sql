-- Security fix (SECURITY_ANALYSIS.md item 2): PIN passa a ser armazenado como hash BCrypt.
-- 1) Amplia a coluna para comportar o hash (60 chars BCrypt).
-- 2) Anula PINs legados em texto puro: usuários afetados refazem o setup-credentials.

ALTER TABLE user_accounts ALTER COLUMN pin TYPE VARCHAR(255);

UPDATE user_accounts
SET pin = NULL
WHERE pin IS NOT NULL
  AND pin NOT LIKE '$2%';
