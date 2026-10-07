CREATE OR REPLACE FUNCTION prevent_retired_bootstrap_account() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    lifecycle installation_state%ROWTYPE;
BEGIN
    SELECT * INTO lifecycle FROM installation_state WHERE id = 1 FOR SHARE;
    IF lifecycle.status = 'COMPLETED'
       AND (lifecycle.bootstrap_account_id = NEW.id OR lower(lifecycle.bootstrap_username) = lower(NEW.username)) THEN
        RAISE EXCEPTION 'Identidade de instalação retirada: %, esperado usuário definitivo distinto', NEW.id;
    END IF;
    RETURN NEW;
END;
$$;
