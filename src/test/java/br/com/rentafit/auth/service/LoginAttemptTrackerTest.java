package br.com.rentafit.auth.service;

import br.com.rentafit.auth.config.LoginLockoutProperties;
import br.com.rentafit.auth.domain.AccountTokenPurpose;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAttemptTracker")
class LoginAttemptTrackerTest {

    private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");
    private static final OffsetDateTime NOW_OFFSET = NOW.atOffset(ZoneOffset.UTC);

    @Mock
    UserAccountRepository accounts;

    @Mock
    AccountLinkIssuer linkIssuer;

    LoginAttemptTracker tracker;
    UserAccount account;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        LoginLockoutProperties properties = new LoginLockoutProperties();
        tracker = new LoginAttemptTracker(accounts, new UnknownUsernameAttempts(properties, clock), linkIssuer,
                properties, clock);
        account = new UserAccount();
        account.setId(UUID.randomUUID());
        account.setUsername("joao.silva");
    }

    private void stubExistingAccount() {
        when(accounts.findByUsername("joao.silva")).thenReturn(Optional.of(account));
        when(accounts.lockById(account.getId())).thenReturn(Optional.of(account));
    }

    @Test
    @DisplayName("1ª falha deixa 4 tentativas restantes")
    void firstFailureLeavesFourAttempts() {
        stubExistingAccount();

        FailedLoginOutcome outcome = tracker.recordFailure("joao.silva");

        assertThat(outcome.remainingAttempts()).isEqualTo(4);
        assertThat(outcome.locked()).isFalse();
        assertThat(account.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("4ª falha deixa exatamente 1 tentativa (gatilho do aviso no front)")
    void fourthFailureLeavesOneAttempt() {
        stubExistingAccount();
        account.setFailedLoginAttempts(3);

        assertThat(tracker.recordFailure("joao.silva").remainingAttempts()).isEqualTo(1);
        verify(linkIssuer, never()).issue(any(), any());
    }

    @Test
    @DisplayName("5ª falha bloqueia por 30 min, zera o contador e envia link de desbloqueio")
    void fifthFailureLocksAccount() {
        stubExistingAccount();
        account.setFailedLoginAttempts(4);

        FailedLoginOutcome outcome = tracker.recordFailure("joao.silva");

        assertThat(outcome.locked()).isTrue();
        assertThat(outcome.lockedUntil()).isEqualTo(NOW_OFFSET.plusMinutes(30));
        assertThat(account.getLockedUntil()).isEqualTo(NOW_OFFSET.plusMinutes(30));
        assertThat(account.getFailedLoginAttempts()).isZero();
        verify(linkIssuer).issue(account, AccountTokenPurpose.UNLOCK);
    }

    @Test
    @DisplayName("conta removida durante o login gera erro com o id")
    void accountRemovedDuringLogin() {
        when(accounts.findByUsername("joao.silva")).thenReturn(Optional.of(account));
        when(accounts.lockById(account.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tracker.recordFailure("joao.silva"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(account.getId().toString());
    }

    @Test
    @DisplayName("username inexistente tem o mesmo contador e bloqueio de uma conta real")
    void unknownUsernameMirrorsRealAccount() {
        when(accounts.findByUsername("fantasma")).thenReturn(Optional.empty());

        for (int i = 0; i < 4; i++) {
            assertThat(tracker.recordFailure("fantasma").locked()).isFalse();
        }
        FailedLoginOutcome fifth = tracker.recordFailure("fantasma");

        assertThat(fifth.locked()).isTrue();
        assertThat(tracker.activeLock("fantasma")).contains(NOW_OFFSET.plusMinutes(30));
        verify(linkIssuer, never()).issue(any(), any());
    }

    @Test
    @DisplayName("activeLock retorna o fim do bloqueio somente enquanto vigente")
    void activeLockOnlyWhileLocked() {
        when(accounts.findByUsername("joao.silva")).thenReturn(Optional.of(account));

        account.setLockedUntil(NOW_OFFSET.plusMinutes(10));
        assertThat(tracker.activeLock("joao.silva")).contains(NOW_OFFSET.plusMinutes(10));

        account.setLockedUntil(NOW_OFFSET.minusSeconds(1));
        assertThat(tracker.activeLock("joao.silva")).isEmpty();
    }

    @Test
    @DisplayName("sucesso zera contador e bloqueio expirado")
    void successResetsCounters() {
        account.setFailedLoginAttempts(3);
        account.setLockedUntil(NOW_OFFSET.minusMinutes(1));
        when(accounts.lockById(account.getId())).thenReturn(Optional.of(account));

        tracker.recordSuccess(account.getId());

        assertThat(account.getFailedLoginAttempts()).isZero();
        assertThat(account.getLockedUntil()).isNull();
    }
}
