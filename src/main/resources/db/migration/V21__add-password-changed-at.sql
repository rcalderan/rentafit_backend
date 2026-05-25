-- V21: Add password_changed_at column for password expiration tracking
ALTER TABLE user_accounts ADD COLUMN password_changed_at TIMESTAMPTZ;
