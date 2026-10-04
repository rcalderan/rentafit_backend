package br.com.rentafit.auth.service;

import br.com.rentafit.auth.dto.OperatorProfileResponseDTO;
import br.com.rentafit.auth.dto.UserProfileResponseDTO;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.people.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperatorIdentityService {
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;

    @Value("${rentafit.security.password-expiry-days:90}")
    private long passwordExpiryDays;

    @Transactional(readOnly = true)
    public OperatorProfileResponseDTO profile(String username) {
        var account = userAccountRepository.findByUsernameWithDetails(username)
                .orElseThrow(() -> new AccessDeniedException("Usuário não habilitado para operar."));
        if (!Boolean.TRUE.equals(account.getIsActive()) || !account.hasOperationalRole()) {
            throw new AccessDeniedException("Usuário não habilitado para operar.");
        }
        var initials = employeeRepository.findInitialsById(account.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não habilitado para operar."));
        var profile = new UserProfileResponseDTO(account, passwordExpiryDays);
        if (!profile.isPinConfigured() || profile.isPasswordExpired()
                || initials.isBlank()) {
            throw new AccessDeniedException("Conclua a configuração de credenciais antes de operar.");
        }
        return new OperatorProfileResponseDTO(profile, initials);
    }
}
