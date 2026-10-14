package br.com.rentafit.auth.exception;

import java.time.OffsetDateTime;

/**
 * Login refused because the account is temporarily locked after repeated failures.
 */
public class AccountLockedException extends RuntimeException {

    private final OffsetDateTime lockedUntil;

    public AccountLockedException(OffsetDateTime lockedUntil) {
        super("Conta bloqueada por excesso de tentativas até " + lockedUntil);
        this.lockedUntil = lockedUntil;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }
}
