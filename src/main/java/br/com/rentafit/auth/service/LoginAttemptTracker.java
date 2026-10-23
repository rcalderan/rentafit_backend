package br.com.rentafit.auth.service;

import br.com.rentafit.auth.config.LoginLockoutProperties;
import br.com.rentafit.auth.domain.AccountTokenPurpose;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Counts consecutive wrong passwords per account and locks it after the configured limit.
 * Each method runs in its own transaction so the counter is persisted even though the login itself fails.
 *
 * <p>Usage: {@code tracker.recordFailure("joao.silva")} after a BadCredentialsException.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptTracker {

    private final UserAccountRepository accounts;
    private final UnknownUsernameAttempts unknownUsernames;
    private final AccountLinkIssuer linkIssuer;
    private final LoginLockoutProperties properties;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<OffsetDateTime> activeLock(String username) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Optional<UserAccount> account = accounts.findByUsername(username);
        if (account.isEmpty()) {
            return unknownUsernames.activeLock(username);
        }
        return Optional.ofNullable(account.get().getLockedUntil()).filter(lockedUntil -> lockedUntil.isAfter(now));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FailedLoginOutcome recordFailure(String username) {
        Optional<UUID> accountId = accounts.findByUsername(username).map(UserAccount::getId);
        if (accountId.isEmpty()) {
            return unknownUsernames.recordFailure(username);
        }
        UserAccount account = accounts.lockById(accountId.get()).orElseThrow(() ->
                new IllegalStateException("Conta " + accountId.get() + " removida durante o login"));
        int failures = account.getFailedLoginAttempts() + 1;
        if (failures < properties.getMaxAttempts()) {
            account.setFailedLoginAttempts(failures);
            return new FailedLoginOutcome(properties.getMaxAttempts() - failures, null);
        }
        return new FailedLoginOutcome(0, lock(account));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID accountId) {
        accounts.lockById(accountId)
                .filter(account -> account.getFailedLoginAttempts() > 0 || account.getLockedUntil() != null)
                .ifPresent(account -> {
                    account.setFailedLoginAttempts(0);
                    account.setLockedUntil(null);
                });
    }

    private OffsetDateTime lock(UserAccount account) {
        OffsetDateTime lockedUntil = OffsetDateTime.now(clock).plus(properties.getLockDuration());
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(lockedUntil);
        log.warn("Conta {} bloqueada até {} após {} tentativas inválidas", account.getId(), lockedUntil,
                properties.getMaxAttempts());
        linkIssuer.issue(account, AccountTokenPurpose.UNLOCK);
        return lockedUntil;
    }
}
