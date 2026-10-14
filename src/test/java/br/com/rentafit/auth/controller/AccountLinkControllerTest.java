package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.dto.AccountLinkTokenRequestDTO;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.dto.ResendVerificationRequestDTO;
import br.com.rentafit.auth.service.AccountLinkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLinkController")
class AccountLinkControllerTest {

    @Mock
    AccountLinkService accountLinkService;

    @InjectMocks
    AccountLinkController controller;

    @Test
    @DisplayName("verify-email devolve 200 com tokens")
    void verifyEmail() {
        LoginResponseDTO tokens = new LoginResponseDTO("a", "r", "Bearer");
        when(accountLinkService.activate("tok")).thenReturn(tokens);

        var response = controller.verifyEmail(new AccountLinkTokenRequestDTO("tok"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(tokens);
    }

    @Test
    @DisplayName("resend-verification devolve 202 sempre")
    void resendVerification() {
        var response = controller.resendVerification(new ResendVerificationRequestDTO("ana@ex.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(accountLinkService).resendActivation("ana@ex.com");
    }

    @Test
    @DisplayName("unlock devolve 204")
    void unlock() {
        var response = controller.unlock(new AccountLinkTokenRequestDTO("tok"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(accountLinkService).unlock("tok");
    }

    @Test
    @DisplayName("DTO de token não expõe o token em toString")
    void tokenDtoRedacted() {
        assertThat(new AccountLinkTokenRequestDTO("secret-token").toString()).doesNotContain("secret-token");
    }
}
