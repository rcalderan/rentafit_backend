package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.AccountTokenPurpose;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redeems e-mailed links: confirms a new account's e-mail and unlocks an account locked by failed logins.
 *
 * <p>Usage: {@code accountLinkService.activate(tokenFromLink)} returns a session for the newly confirmed user.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLinkService {

    private final AccountLinkIssuer linkIssuer;
    private final UserAccountRepository accounts;
    private final LoginSessionIssuer sessionIssuer;

    /** Confirms the e-mail and logs the user in, so the frontend can continue to password/PIN setup. */
    @Transactional
    public LoginResponseDTO activate(String rawToken) {
        UserAccount account = linkIssuer.redeem(rawToken, AccountTokenPurpose.ACTIVATION);
        account.setEmailVerificationPending(false);
        log.info("E-mail confirmado para a conta {}", account.getId());
        return sessionIssuer.issue(account);
    }

    /** Always silent about whether the e-mail exists, to avoid account enumeration. */
    @Transactional
    public void resendActivation(String email) {
        accounts.findAllByPersonEmail(email.trim()).stream()
                .filter(UserAccount::isEmailVerificationPending)
                .forEach(account -> linkIssuer.issue(account, AccountTokenPurpose.ACTIVATION));
    }

    @Transactional
    public void unlock(String rawToken) {
        UserAccount account = linkIssuer.redeem(rawToken, AccountTokenPurpose.UNLOCK);
        account.setLockedUntil(null);
        account.setFailedLoginAttempts(0);
        log.info("Conta {} desbloqueada via link de e-mail", account.getId());
    }
}
