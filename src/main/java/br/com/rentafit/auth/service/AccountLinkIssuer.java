package br.com.rentafit.auth.service;

import br.com.rentafit.auth.config.AccountLinkProperties;
import br.com.rentafit.auth.domain.AccountTokenPurpose;
import br.com.rentafit.auth.domain.AccountVerificationToken;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.event.AccountLinkMailRequestedEvent;
import br.com.rentafit.auth.repository.AccountVerificationTokenRepository;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.domain.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues and redeems the single-use links e-mailed for account activation and unlock.
 *
 * <p>Usage: {@code linkIssuer.issue(account, AccountTokenPurpose.UNLOCK)} inside the transaction that locked the account.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLinkIssuer {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountVerificationTokenRepository tokens;
    private final AccountLinkProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public void issue(UserAccount account, AccountTokenPurpose purpose) {
        String email = account.getPerson() == null ? null : account.getPerson().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Link {} não enviado: conta {} sem e-mail cadastrado", purpose, account.getId());
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        tokens.revokeActive(account.getId(), purpose, now);
        String rawToken = newRawToken();
        OffsetDateTime expiresAt = now.plus(ttlOf(purpose));
        tokens.save(AccountVerificationToken.builder().userAccount(account).tokenHash(sha256(rawToken))
                .purpose(purpose).expiresAt(expiresAt).createdAt(now).build());
        events.publishEvent(new AccountLinkMailRequestedEvent(purpose, email, nameOf(account.getPerson()), rawToken, expiresAt));
    }

    @Transactional
    public UserAccount redeem(String rawToken, AccountTokenPurpose purpose) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        AccountVerificationToken token = tokens.findByTokenHashAndPurpose(sha256(rawToken), purpose)
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(() -> new ValidationException(
                        "Link de " + (purpose == AccountTokenPurpose.ACTIVATION ? "ativação" : "desbloqueio")
                                + " inválido, expirado ou já utilizado; solicite um novo"));
        token.setUsedAt(now);
        return token.getUserAccount();
    }

    private Duration ttlOf(AccountTokenPurpose purpose) {
        return switch (purpose) {
            case ACTIVATION -> properties.getActivationTtl();
            case UNLOCK -> properties.getUnlockTtl();
        };
    }

    private static String nameOf(Person person) {
        return person.getName() == null ? "" : person.getName();
    }

    private static String newRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
