package br.com.rentafit.auth.service;

import br.com.rentafit.auth.config.LoginLockoutProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Mirrors the lockout counter for usernames that do not exist, so responses (remaining attempts, lock)
 * are identical for real and fake accounts and cannot be used to enumerate users.
 * Kept in memory: losing it on restart only resets counters for non-existent users.
 */
@Component
public class UnknownUsernameAttempts {

    private record Attempts(int failures, OffsetDateTime lockedUntil) {
    }

    private final Cache<String, Attempts> attempts;
    private final LoginLockoutProperties properties;
    private final Clock clock;

    public UnknownUsernameAttempts(LoginLockoutProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.attempts = Caffeine.newBuilder()
                .expireAfterWrite(properties.getLockDuration())
                .maximumSize(properties.getUnknownUsernameCacheSize())
                .build();
    }

    public Optional<OffsetDateTime> activeLock(String username) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return Optional.ofNullable(attempts.getIfPresent(key(username)))
                .map(Attempts::lockedUntil)
                .filter(lockedUntil -> lockedUntil.isAfter(now));
    }

    /** Returns the outcome with the same semantics as a real account. */
    public FailedLoginOutcome recordFailure(String username) {
        Attempts updated = attempts.asMap().compute(key(username), (ignored, current) -> next(current));
        int remaining = Math.max(0, properties.getMaxAttempts() - updated.failures());
        return new FailedLoginOutcome(remaining, updated.lockedUntil());
    }

    private Attempts next(Attempts current) {
        int failures = (current == null || current.lockedUntil() != null ? 0 : current.failures()) + 1;
        if (failures < properties.getMaxAttempts()) {
            return new Attempts(failures, null);
        }
        return new Attempts(failures, OffsetDateTime.now(clock).plus(properties.getLockDuration()));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
