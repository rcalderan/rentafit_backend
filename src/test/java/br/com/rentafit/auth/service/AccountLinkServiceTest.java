package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.AccountTokenPurpose;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLinkService")
class AccountLinkServiceTest {

    @Mock
    AccountLinkIssuer linkIssuer;

    @Mock
    UserAccountRepository accounts;

    @Mock
    LoginSessionIssuer sessionIssuer;

    @InjectMocks
    AccountLinkService service;

    UserAccount account;

    @BeforeEach
    void setUp() {
        account = new UserAccount();
        account.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("activate confirma o e-mail e devolve sessão")
    void activateConfirmsEmail() {
        account.setEmailVerificationPending(true);
        LoginResponseDTO tokens = new LoginResponseDTO("a", "r", "Bearer");
        when(linkIssuer.redeem("tok", AccountTokenPurpose.ACTIVATION)).thenReturn(account);
        when(sessionIssuer.issue(account)).thenReturn(tokens);

        assertThat(service.activate("tok")).isEqualTo(tokens);
        assertThat(account.isEmailVerificationPending()).isFalse();
        assertThat(account.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("resend envia novo link apenas para contas pendentes")
    void resendOnlyPendingAccounts() {
        UserAccount confirmed = new UserAccount();
        account.setEmailVerificationPending(true);
        when(accounts.findAllByPersonEmail("ana@ex.com")).thenReturn(List.of(account, confirmed));

        service.resendActivation(" ana@ex.com ");

        verify(linkIssuer).issue(account, AccountTokenPurpose.ACTIVATION);
        verify(linkIssuer, never()).issue(confirmed, AccountTokenPurpose.ACTIVATION);
    }

    @Test
    @DisplayName("resend para e-mail desconhecido não faz nada (sem enumeração)")
    void resendUnknownEmailIsSilent() {
        when(accounts.findAllByPersonEmail("x@ex.com")).thenReturn(List.of());

        service.resendActivation("x@ex.com");

        verify(linkIssuer, never()).issue(any(), any());
    }

    @Test
    @DisplayName("unlock remove bloqueio e zera contador")
    void unlockClearsLock() {
        account.setLockedUntil(OffsetDateTime.now().plusMinutes(20));
        account.setFailedLoginAttempts(2);
        when(linkIssuer.redeem("tok", AccountTokenPurpose.UNLOCK)).thenReturn(account);

        service.unlock("tok");

        assertThat(account.getLockedUntil()).isNull();
        assertThat(account.getFailedLoginAttempts()).isZero();
        assertThat(account.isAccountNonLocked()).isTrue();
    }
}
