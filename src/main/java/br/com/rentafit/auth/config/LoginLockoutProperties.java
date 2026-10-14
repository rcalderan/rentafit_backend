package br.com.rentafit.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Brute-force protection for password login: after {@code maxAttempts} consecutive failures the account
 * is locked for {@code lockDuration}.
 */
@Data
@Component
@ConfigurationProperties(prefix = "rentafit.security.login")
public class LoginLockoutProperties {

    private int maxAttempts = 5;

    private Duration lockDuration = Duration.ofMinutes(30);

    /** Upper bound of distinct unknown usernames tracked in memory, so username-spraying cannot exhaust the heap. */
    private long unknownUsernameCacheSize = 10_000;
}
