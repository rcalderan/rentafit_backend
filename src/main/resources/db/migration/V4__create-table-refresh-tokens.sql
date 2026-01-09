-- Migration to create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    expiry_date TIMESTAMPTZ NOT NULL,
    user_account_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_account_id ON refresh_tokens(user_account_id);

