package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.dto.OperatorLoginResponseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationLogSafetyTest {
    @Test
    void loginRequestDoesNotExposePasswordInLogs() {
        assertThat(new LoginRequestDTO("test-user", "test-password").toString())
                .contains("REDACTED").doesNotContain("test-password");
    }

    @Test
    void tokenResponsesDoNotExposeTokensInLogs() {
        assertThat(new LoginResponseDTO("test-access", "test-refresh", "Bearer").toString())
                .contains("REDACTED").doesNotContain("test-access", "test-refresh");
        assertThat(new OperatorLoginResponseDTO("test-access", "test-refresh", "Bearer", null).toString())
                .contains("REDACTED").doesNotContain("test-access", "test-refresh");
    }
}
