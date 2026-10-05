package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.domain.InstallationState;
import br.com.rentafit.auth.dto.UserProfileResponseDTO;
import br.com.rentafit.auth.repository.InstallationStateRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstallationCompletionService {
    private final InstallationStateRepository installationRepository;
    private final UserAccountRepository accountRepository;
    private final RefreshTokenService refreshTokens;
    private final UserAccountService accounts;
    private final OperatorIdentityService operators;

    @Transactional
    public void afterVerifiedLogin(UserAccount authenticated) {
        if (!isAdministrator(authenticated)) return;
        var optionalState = installationRepository.lockInstallation();
        if (optionalState.isEmpty() || !"PENDING".equals(optionalState.get().getStatus())) return;
        InstallationState state = optionalState.get();
        if (state.getBootstrapAccountId().equals(authenticated.getId())) return;
        UserAccount candidate = accountRepository.lockById(authenticated.getId()).orElseThrow(() ->
                new AccessDeniedException("Administrador definitivo não está ativo"));
        if (!isReady(candidate)) return;
        retireBootstrap(state, candidate.getId());
    }

    private boolean isAdministrator(UserAccount account) {
        return account != null && Boolean.TRUE.equals(account.getIsActive()) && account.getRoles() != null
                && account.getRoles().stream().anyMatch(role -> role.getRole() == RoleName.ADMIN);
    }

    private boolean isReady(UserAccount candidate) {
        if (!isAdministrator(candidate) || candidate.getPin() == null || candidate.getPasswordChangedAt() == null) return false;
        if (new UserProfileResponseDTO(candidate, accounts.getPasswordExpiryDays()).isPasswordExpired()) return false;
        try {
            var verified = operators.profile(candidate.getUsername());
            return verified != null && candidate.getId().equals(verified.user().getId());
        } catch (AccessDeniedException exception) {
            return false;
        }
    }

    private void retireBootstrap(InstallationState state, UUID candidateId) {
        accountRepository.lockById(state.getBootstrapAccountId()).ifPresent(bootstrap -> {
            refreshTokens.deleteByUserId(bootstrap.getId());
            accountRepository.delete(bootstrap);
        });
        state.setStatus("COMPLETED");
        state.setCompletedByAccountId(candidateId);
        state.setCompletedAt(OffsetDateTime.now());
        installationRepository.save(state);
        accountRepository.flush();
        installationRepository.flush();
    }
}
