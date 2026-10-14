package br.com.rentafit.auth.service;

import java.time.OffsetDateTime;

/**
 * Result of recording a wrong password: attempts left, or the lock end when this failure triggered the lock.
 */
public record FailedLoginOutcome(int remainingAttempts, OffsetDateTime lockedUntil) {

    public boolean locked() {
        return lockedUntil != null;
    }
}
