package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserAccountService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${rentafit.security.password-expiry-days:90}")
    private long passwordExpiryDays;

    public long getPasswordExpiryDays() {
        return passwordExpiryDays;
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccount loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findUserProfile(@Param("username") String username){
        return userAccountRepository.findByUsernameWithDetails(username);
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> getUserWithDetails(String username) {
        return userAccountRepository.findByUsernameWithDetails(username);
    }

    /**
     * First-access setup: sets password and PIN.
     * Only allowed when the user's PIN is still null (first access).
     */
    @Transactional
    public void setupCredentials(UserAccount user, String newPassword, String pin) {
        if (user.getPin() != null) {
            throw new ValidationException("Credentials already configured. Use change-password to update your password.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPin(passwordEncoder.encode(pin));
        user.setPasswordChangedAt(OffsetDateTime.now());
        userAccountRepository.save(user);
    }

    /**
     * Changes the password for an authenticated user (expired password flow).
     */
    @Transactional
    public void changePassword(UserAccount user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(OffsetDateTime.now());
        userAccountRepository.save(user);
    }

    /**
     * Vincula o emitente (CNPJ) ao usuário autenticado.
     */
    @Transactional
    public UserAccount setupIssuerCnpj(UserAccount user, String issuerCnpj) {
        if (issuerCnpj == null || !issuerCnpj.matches("\\d{14}")) {
            throw new ValidationException("CNPJ deve conter exatamente 14 dígitos numéricos.");
        }
        UserAccount managed = userAccountRepository.findByUsernameWithDetails(user.getUsername())
                .orElseThrow(() -> new ValidationException("Usuário não encontrado: " + user.getUsername()));
        managed.setIssuerCnpj(issuerCnpj);
        return userAccountRepository.save(managed);
    }
}

