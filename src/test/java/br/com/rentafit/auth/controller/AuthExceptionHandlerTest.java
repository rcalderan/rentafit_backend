package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.exception.AccountLockedException;
import br.com.rentafit.auth.exception.EmailNotVerifiedException;
import br.com.rentafit.auth.exception.InvalidCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthExceptionHandler")
class AuthExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");

    private final AuthExceptionHandler handler = new AuthExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC));
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

    @Test
    @DisplayName("credenciais inválidas: 403 INVALID_CREDENTIALS com remainingAttempts")
    void invalidCredentials() {
        var response = handler.handleInvalidCredentials(new InvalidCredentialsException(1), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().getDetails()).containsEntry("remainingAttempts", 1);
    }

    @Test
    @DisplayName("conta bloqueada: 423 ACCOUNT_LOCKED com lockedUntil e Retry-After em segundos")
    void accountLocked() {
        OffsetDateTime lockedUntil = NOW.atOffset(ZoneOffset.UTC).plusMinutes(30);

        var response = handler.handleAccountLocked(new AccountLockedException(lockedUntil), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        assertThat(response.getBody().getCode()).isEqualTo("ACCOUNT_LOCKED");
        assertThat(response.getBody().getDetails()).containsEntry("lockedUntil", lockedUntil.toString());
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1800");
    }

    @Test
    @DisplayName("e-mail não verificado: 403 EMAIL_NOT_VERIFIED")
    void emailNotVerified() {
        var response = handler.handleEmailNotVerified(new EmailNotVerifiedException("joao"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getCode()).isEqualTo("EMAIL_NOT_VERIFIED");
        assertThat(response.getBody().getPath()).isEqualTo("/api/auth/login");
    }
}
