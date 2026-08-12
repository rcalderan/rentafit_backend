package br.com.rentafit.people.service;

import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.repository.RoleRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.auth.service.RefreshTokenService;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.common.security.TokenService;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.CustomerDetailsDTO;
import br.com.rentafit.people.dto.SignUpRequestDTO;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates public self-registration: persists a Customer, creates the linked
 * UserAccount with CUSTOMER role (random password, null PIN) and returns auth tokens
 * so the front-end can route the user straight to /auth/setup-credentials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final CustomerService customerService;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResponseDTO registerCustomer(SignUpRequestDTO dto) {
        String username = dto.email().trim().toLowerCase();
        ensureUsernameAvailable(username);

        CustomerDetailsDTO customer = customerService.create(toCustomerDTO(dto, username));

        createCustomerUserAccount(customer.id(), username);
        entityManager.flush();

        UserAccount account = userAccountRepository.findById(customer.id())
                .orElseThrow(() -> new ValidationException(
                        "User account not found after creation for id=" + customer.id()));

        String accessToken = tokenService.generateToken(username);
        String refreshToken = refreshTokenService.createRefreshToken(account).getToken();

        log.info("Self-registered customer with id: {}", customer.id());
        return new LoginResponseDTO(accessToken, refreshToken, "Bearer");
    }

    private void ensureUsernameAvailable(String username) {
        userAccountRepository.findByUsername(username).ifPresent(existing -> {
            throw new ValidationException("An account with email '" + username + "' already exists");
        });
    }

    private CustomerDTO toCustomerDTO(SignUpRequestDTO dto, String username) {
        return CustomerDTO.builder()
                .name(dto.name())
                .email(username)
                .document(dto.document())
                .phones(dto.phones())
                .address(dto.address())
                .number(dto.number())
                .complement(dto.complement())
                .isAuthenticated(true)
                .build();
    }

    private void createCustomerUserAccount(UUID customerId, String username) {
        Role customerRole = roleRepository.findByRole(RoleName.CUSTOMER)
                .orElseThrow(() -> new ValidationException("CUSTOMER role not configured"));

        String randomPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        entityManager.createNativeQuery(
                        "INSERT INTO user_accounts (id, username, password, is_active) VALUES (:id, :username, :password, :active)")
                .setParameter("id", customerId)
                .setParameter("username", username)
                .setParameter("password", randomPasswordHash)
                .setParameter("active", true)
                .executeUpdate();

        entityManager.createNativeQuery("INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId)")
                .setParameter("userId", customerId)
                .setParameter("roleId", customerRole.getId())
                .executeUpdate();
    }
}
