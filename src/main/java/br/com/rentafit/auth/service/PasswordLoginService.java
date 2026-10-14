package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.exception.AccountLockedException;
import br.com.rentafit.auth.exception.EmailNotVerifiedException;
import br.com.rentafit.auth.exception.InvalidCredentialsException;
import br.com.rentafit.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Username/password login with brute-force lockout.
 * Deliberately not @Transactional: a failed authentication must not mark an outer transaction rollback-only,
 * and the attempt counter is persisted by {@link LoginAttemptTracker} in its own transaction.
 *
 * <p>Usage: {@code passwordLoginService.login(new LoginRequestDTO("joao.silva", "secret"))}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordLoginService {

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptTracker attempts;
    private final LoginSessionIssuer sessionIssuer;
    private final UserAccountRepository accounts;

    public LoginResponseDTO login(LoginRequestDTO request) {
        log.info("Tentativa de login para username: {}", request.username());
        attempts.activeLock(request.username()).ifPresent(lockedUntil -> {
            throw new AccountLockedException(lockedUntil);
        });
        UserAccount account = authenticate(request);
        attempts.recordSuccess(account.getId());
        log.info("Usuário autenticado: {} com roles: {}", account.getUsername(), account.getAuthorities());
        return sessionIssuer.issue(account);
    }

    private UserAccount authenticate(LoginRequestDTO request) {
        if (BCRYPT_PATTERN.matcher(request.password()).matches()) {
            log.warn("Login negado: senha recebida em formato BCrypt hash para username: {}", request.username());
            throw failure(request.username());
        }
        try {
            var token = new UsernamePasswordAuthenticationToken(request.username(), request.password());
            return (UserAccount) authenticationManager.authenticate(token).getPrincipal();
        } catch (BadCredentialsException e) {
            log.warn("Falha na autenticação para username: {}", request.username());
            throw failure(request.username());
        } catch (LockedException e) {
            throw attempts.activeLock(request.username()).map(AccountLockedException::new)
                    .orElseThrow(() -> e);
        } catch (DisabledException e) {
            if (isAwaitingEmailVerification(request.username())) {
                throw new EmailNotVerifiedException(request.username());
            }
            throw failure(request.username());
        }
    }

    private boolean isAwaitingEmailVerification(String username) {
        return accounts.findByUsername(username).map(UserAccount::isEmailVerificationPending).orElse(false);
    }

    private RuntimeException failure(String username) {
        FailedLoginOutcome outcome = attempts.recordFailure(username);
        return outcome.locked()
                ? new AccountLockedException(outcome.lockedUntil())
                : new InvalidCredentialsException(outcome.remainingAttempts());
    }
}
