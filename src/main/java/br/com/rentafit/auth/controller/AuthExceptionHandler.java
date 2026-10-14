package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.exception.AccountLockedException;
import br.com.rentafit.auth.exception.EmailNotVerifiedException;
import br.com.rentafit.auth.exception.InvalidCredentialsException;
import br.com.rentafit.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Maps login failures to responses the login screen can act on (warning before lock, lock end, e-mail pending).
 * Ordered first because GlobalExceptionHandler has a catch-all Exception handler that would otherwise win.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class AuthExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        var body = body(HttpStatus.FORBIDDEN, "INVALID_CREDENTIALS", "Usuário ou senha inválidos.", request,
                Map.of("remainingAttempts", ex.getRemainingAttempts()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex, HttpServletRequest request) {
        var body = body(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                "Conta bloqueada temporariamente por excesso de tentativas.", request,
                Map.of("lockedUntil", ex.getLockedUntil().toString()));
        long retryAfterSeconds = Math.max(1, Duration.between(OffsetDateTime.now(clock), ex.getLockedUntil()).toSeconds());
        return ResponseEntity.status(HttpStatus.LOCKED)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds))
                .body(body);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(EmailNotVerifiedException ex, HttpServletRequest request) {
        log.info("Login recusado em {}: {}", request.getRequestURI(), ex.getMessage());
        var body = body(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                "Confirme seu e-mail para acessar. Verifique sua caixa de entrada.", request, null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    private ErrorResponse body(HttpStatus status, String code, String message, HttpServletRequest request,
                               Map<String, Object> details) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now(clock))
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .details(details)
                .path(request.getRequestURI())
                .build();
    }
}
