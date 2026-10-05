CREATE TABLE installation_state (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    status VARCHAR(20) NOT NULL CHECK (status IN ('LEGACY', 'PENDING', 'COMPLETED')),
    bootstrap_account_id UUID NOT NULL,
    bootstrap_username VARCHAR(50) NOT NULL,
    completed_by_account_id UUID,
    completed_at TIMESTAMPTZ
);
INSERT INTO installation_state (id, status, bootstrap_account_id, bootstrap_username)
VALUES (1, 'LEGACY', '0194269a-0000-7000-8000-000000000001', 'admin');

CREATE FUNCTION prevent_retired_bootstrap_account() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM installation_state
               WHERE status = 'COMPLETED'
                 AND (bootstrap_account_id = NEW.id OR lower(bootstrap_username) = lower(NEW.username))) THEN
        RAISE EXCEPTION 'Identidade de instalação retirada: %, esperado usuário definitivo distinto', NEW.id;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER guard_retired_bootstrap_account
BEFORE INSERT OR UPDATE ON user_accounts
FOR EACH ROW EXECUTE FUNCTION prevent_retired_bootstrap_account();
