package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.exception.AccountLockedException;
import br.com.rentafit.auth.exception.EmailNotVerifiedException;
import br.com.rentafit.auth.exception.InvalidCredentialsException;
import br.com.rentafit.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordLoginService")
class PasswordLoginServiceTest {

    private static final OffsetDateTime LOCKED_UNTIL = OffsetDateTime.parse("2026-10-12T12:30:00Z");

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    LoginAttemptTracker attempts;

    @Mock
    LoginSessionIssuer sessionIssuer;

    @Mock
    UserAccountRepository accounts;

    @InjectMocks
    PasswordLoginService service;

    UserAccount account;
    LoginRequestDTO request;

    @BeforeEach
    void setUp() {
        account = new UserAccount();
        account.setId(UUID.randomUUID());
        account.setUsername("joao.silva");
        request = new LoginRequestDTO("joao.silva", "Vestido#Azul42");
    }

    @Test
    @DisplayName("login válido zera tentativas e emite sessão")
    void successfulLogin() {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(account, null, account.getAuthorities()));
        LoginResponseDTO tokens = new LoginResponseDTO("access", "refresh", "Bearer");
        when(sessionIssuer.issue(account)).thenReturn(tokens);

        assertThat(service.login(request)).isEqualTo(tokens);
        verify(attempts).recordSuccess(account.getId());
    }

    @Test
    @DisplayName("conta já bloqueada é recusada antes de verificar a senha")
    void lockedAccountRefusedBeforePasswordCheck() {
        when(attempts.activeLock("joao.silva")).thenReturn(Optional.of(LOCKED_UNTIL));

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOfSatisfying(AccountLockedException.class, e -> assertThat(e.getLockedUntil()).isEqualTo(LOCKED_UNTIL));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("senha errada registra falha e informa tentativas restantes")
    void wrongPasswordReportsRemainingAttempts() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        when(attempts.recordFailure("joao.silva")).thenReturn(new FailedLoginOutcome(1, null));

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOfSatisfying(InvalidCredentialsException.class, e -> assertThat(e.getRemainingAttempts()).isEqualTo(1));
    }

    @Test
    @DisplayName("falha que dispara o bloqueio responde como conta bloqueada")
    void failureThatLocksReportsLock() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        when(attempts.recordFailure("joao.silva")).thenReturn(new FailedLoginOutcome(0, LOCKED_UNTIL));

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("senha em formato hash BCrypt conta como falha sem chegar ao AuthenticationManager")
    void bcryptHashCountsAsFailure() {
        when(attempts.recordFailure("joao.silva")).thenReturn(new FailedLoginOutcome(4, null));

        assertThatThrownBy(() -> service.login(new LoginRequestDTO("joao.silva", "$2a$10$" + "a".repeat(53))))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("LockedException do provider vira AccountLockedException com o fim do bloqueio")
    void providerLockedException() {
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));
        when(attempts.activeLock("joao.silva")).thenReturn(Optional.empty(), Optional.of(LOCKED_UNTIL));

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("LockedException sem bloqueio vigente é propagada")
    void providerLockedExceptionWithoutActiveLock() {
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(LockedException.class);
    }

    @Test
    @DisplayName("conta aguardando confirmação de e-mail recebe EMAIL_NOT_VERIFIED")
    void pendingEmailVerification() {
        account.setEmailVerificationPending(true);
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));
        when(accounts.findByUsername("joao.silva")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(EmailNotVerifiedException.class);
    }

    @Test
    @DisplayName("conta desativada não revela o motivo: responde credenciais inválidas")
    void deactivatedAccountLooksLikeInvalidCredentials() {
        account.setIsActive(false);
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));
        when(accounts.findByUsername("joao.silva")).thenReturn(Optional.of(account));
        when(attempts.recordFailure("joao.silva")).thenReturn(new FailedLoginOutcome(4, null));

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("falha de infraestrutura não é mascarada como credencial inválida")
    void infrastructureFailurePropagates() {
        when(authenticationManager.authenticate(any())).thenThrow(new AuthenticationServiceException("db down"));

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(AuthenticationServiceException.class);
        verify(attempts, never()).recordFailure(any());
    }
}
