package br.com.rentafit.people.service;

import br.com.rentafit.auth.domain.AccountTokenPurpose;
import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.RoleRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.auth.service.AccountLinkIssuer;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.common.mail.OutgoingMail;
import br.com.rentafit.common.validation.UsernameRules;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.dto.CustomerDetailsDTO;
import br.com.rentafit.people.dto.SignUpRequestDTO;
import br.com.rentafit.people.dto.SignUpResponseDTO;
import br.com.rentafit.people.repository.CustomerRepository;
import br.com.rentafit.people.util.DocumentUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Orchestrates public self-registration: persists a Customer, creates the linked
 * UserAccount with CUSTOMER role (chosen username, random password, null PIN) pending e-mail
 * confirmation, and e-mails the activation link. The user only gets a session after confirming
 * the e-mail, then finishes password/PIN setup at /auth/setup-credentials.
 * If a customer with the same document already exists (e.g., migrated from legacy),
 * the registration updates the existing record instead of failing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final AccountLinkIssuer accountLinkIssuer;

    @Transactional
    public SignUpResponseDTO registerCustomer(SignUpRequestDTO dto) {
        String username = UsernameRules.normalize(dto.username());
        String email = dto.email().trim().toLowerCase(Locale.ROOT);
        String document = DocumentUtils.normalize(dto.document());

        CustomerDetailsDTO customer = customerRepository.findByDocument(document)
                .map(existing -> mergeLegacyCustomer(existing, dto, username, email))
                .orElseGet(() -> createNewCustomer(dto, username, email, document));

        createOrUpdateCustomerUserAccount(customer.id(), username);
        entityManager.flush();
        entityManager.clear();

        UserAccount account = userAccountRepository.findById(customer.id())
                .orElseThrow(() -> new ValidationException(
                        "User account not found after creation for id=" + customer.id()));
        accountLinkIssuer.issue(account, AccountTokenPurpose.ACTIVATION);

        log.info("Self-registered customer with id: {} (aguardando confirmação de e-mail)", customer.id());
        return new SignUpResponseDTO(OutgoingMail.maskEmail(email));
    }

    private CustomerDetailsDTO createNewCustomer(SignUpRequestDTO dto, String username, String email, String document) {
        ensureIdentifiersAvailable(username, email, null);
        return customerService.create(toCustomerDTO(dto, email, document));
    }

    private CustomerDetailsDTO mergeLegacyCustomer(Customer existing, SignUpRequestDTO dto, String username, String email) {
        ensureIdentifiersAvailable(username, email, existing.getId());
        return customerService.updateFromSignUp(existing, dto);
    }

    private void ensureIdentifiersAvailable(String username, String email, UUID exceptForId) {
        userAccountRepository.findByUsernameIgnoreCase(username)
                .filter(other -> !other.getId().equals(exceptForId))
                .ifPresent(other -> {
                    throw new ValidationException("Username '" + username + "' já está em uso; escolha outro");
                });
        boolean emailTaken = userAccountRepository.findAllByPersonEmail(email).stream()
                .anyMatch(other -> !other.getId().equals(exceptForId));
        if (emailTaken) {
            throw new ValidationException("An account with email '" + email + "' already exists");
        }
    }

    private br.com.rentafit.people.dto.CustomerDTO toCustomerDTO(SignUpRequestDTO dto, String email, String document) {
        return br.com.rentafit.people.dto.CustomerDTO.builder()
                .name(dto.name())
                .email(email)
                .document(document)
                .phones(dto.phones())
                .address(dto.address())
                .number(dto.number())
                .complement(dto.complement())
                .isAuthenticated(true)
                .build();
    }

    private void createOrUpdateCustomerUserAccount(UUID customerId, String username) {
        Role customerRole = roleRepository.findByRole(RoleName.CUSTOMER)
                .orElseThrow(() -> new ValidationException("CUSTOMER role not configured"));

        String randomPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());

        // Upsert user_account (insert or update the password if it already exists).
        // email_verification_pending=true keeps the account disabled until the e-mail link is confirmed.
        entityManager.createNativeQuery(
                        "INSERT INTO user_accounts (id, username, password, is_active, failed_login_attempts, "
                                + "email_verification_pending) "
                                + "VALUES (:id, :username, :password, :active, 0, true) "
                                + "ON CONFLICT (id) DO UPDATE SET "
                                + "username = EXCLUDED.username, "
                                + "password = EXCLUDED.password, "
                                + "is_active = EXCLUDED.is_active, "
                                + "email_verification_pending = true")
                .setParameter("id", customerId)
                .setParameter("username", username)
                .setParameter("password", randomPasswordHash)
                .setParameter("active", true)
                .executeUpdate();

        entityManager.createNativeQuery(
                        "INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId) " +
                                "ON CONFLICT DO NOTHING")
                .setParameter("userId", customerId)
                .setParameter("roleId", customerRole.getId())
                .executeUpdate();
    }
}
