-- Bloqueio por tentativas de login e ativação de conta por e-mail.
-- email_verification_pending usa DEFAULT FALSE para que contas criadas fora do signup
-- (funcionários, bootstrap, scripts de migração) continuem habilitadas sem alteração.
ALTER TABLE user_accounts
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMPTZ,
    ADD COLUMN email_verification_pending BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX ux_user_accounts_username_lower ON user_accounts (lower(username));

CREATE TABLE account_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_accounts (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    purpose VARCHAR(20) NOT NULL CHECK (purpose IN ('ACTIVATION', 'UNLOCK')),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_account_verification_tokens_user_purpose ON account_verification_tokens (user_id, purpose);
