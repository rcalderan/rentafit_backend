package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.people.domain.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OperatorIdentityService.class)
class OperatorIdentityPersistenceTest {
    @Test
    void acceptsOperatorWhenPersonIsAlreadyManagedAsCustomer(@Autowired TestEntityManager entities,
                                                            @Autowired OperatorIdentityService service) {
        var account = customerWithEmployeeRow(entities, RoleName.ADMIN);
        assertThat(account.getPerson()).isInstanceOf(Customer.class);

        var profile = service.profile(account.getUsername());

        assertThat(profile.initials()).isEqualTo("OP");
        assertThat(profile.user().getId()).isEqualTo(account.getId());
        assertThat(profile.user().getRoles()).contains("ADMIN");
    }

    @Test
    void acceptsOperatorInFreshPersistenceContext(@Autowired TestEntityManager entities,
                                                 @Autowired OperatorIdentityService service) {
        var account = customerWithEmployeeRow(entities, RoleName.EMPLOYEE);
        var username = account.getUsername();
        var id = account.getId();
        entities.clear();

        var profile = service.profile(username);

        assertThat(profile.initials()).isEqualTo("OP");
        assertThat(profile.user().getId()).isEqualTo(id);
    }

    @Test
    void customerRoleIsStillDeniedEvenWithEmployeeRow(@Autowired TestEntityManager entities,
                                                     @Autowired OperatorIdentityService service) {
        var account = customerWithEmployeeRow(entities, RoleName.CUSTOMER);

        assertThatThrownBy(() -> service.profile(account.getUsername())).isInstanceOf(AccessDeniedException.class);
    }

    private UserAccount customerWithEmployeeRow(TestEntityManager entities, RoleName roleName) {
        var customer = new Customer();
        customer.setName("Test Operator");
        customer.setEmail("operator@example.test");
        customer.setPhones(List.of("0000000000"));
        entities.persistAndFlush(customer);
        var role = entities.persistAndFlush(new Role(null, roleName));
        var account = new UserAccount(customer, "test-password-hash");
        account.setUsername("test-operator");
        account.setPin("test-pin-hash");
        account.setPasswordChangedAt(OffsetDateTime.now());
        account.setRoles(List.of(role));
        entities.persistAndFlush(account);
        entities.getEntityManager().createNativeQuery("INSERT INTO employees (id, initials, role_level) VALUES (:id, 'OP', 1)")
                .setParameter("id", customer.getId()).executeUpdate();
        return account;
    }
}
