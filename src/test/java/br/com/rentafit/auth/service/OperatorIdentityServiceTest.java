package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperatorIdentityServiceTest {
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private EmployeeRepository employeeRepository;
    @InjectMocks private OperatorIdentityService service;
    private UserAccount account;
    private Employee employee;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "passwordExpiryDays", 90L);
        account = new UserAccount();
        account.setId(UUID.randomUUID());
        account.setUsername("operator");
        account.setPin("hashed-pin");
        account.setPasswordChangedAt(OffsetDateTime.now());
        account.setRoles(List.of(new Role(1L, RoleName.EMPLOYEE)));
        employee = new Employee();
        employee.setId(account.getId());
        employee.setInitials("OP");
        employee.setName("Operator");
        account.setPerson(employee);
        when(userAccountRepository.findByUsernameWithDetails("operator")).thenReturn(Optional.of(account));
    }

    @ParameterizedTest
    @EnumSource(value = RoleName.class, names = {"EMPLOYEE", "MANAGER", "ADMIN"})
    void acceptsOperationalRoles(RoleName role) {
        account.setRoles(List.of(new Role(1L, role)));
        when(employeeRepository.findInitialsById(account.getId())).thenReturn(Optional.of(employee.getInitials()));

        var profile = service.profile("operator");

        assertThat(profile.initials()).isEqualTo("OP");
        assertThat(profile.user().getId()).isEqualTo(account.getId());
        assertThat(profile.user().getRoles()).contains(role.name());
    }

    @Test
    void rejectsCustomerWithoutReadingEmployee() {
        account.setRoles(List.of(new Role(1L, RoleName.CUSTOMER)));
        assertThatThrownBy(() -> service.profile("operator")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void rejectsDisabledAccount() {
        account.setIsActive(false);
        assertThatThrownBy(() -> service.profile("operator")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void rejectsMissingEmployeeRow() {
        when(employeeRepository.findInitialsById(account.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.profile("operator")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsExpiredPassword() {
        account.setPasswordChangedAt(OffsetDateTime.now().minusDays(91));
        when(employeeRepository.findInitialsById(account.getId())).thenReturn(Optional.of(employee.getInitials()));
        assertThatThrownBy(() -> service.profile("operator")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsMissingPin() {
        account.setPin(null);
        when(employeeRepository.findInitialsById(account.getId())).thenReturn(Optional.of(employee.getInitials()));
        assertThatThrownBy(() -> service.profile("operator")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsMissingInitials() {
        employee.setInitials(" ");
        when(employeeRepository.findInitialsById(account.getId())).thenReturn(Optional.of(employee.getInitials()));
        assertThatThrownBy(() -> service.profile("operator")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsMissingAccount() {
        when(userAccountRepository.findByUsernameWithDetails("operator")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.profile("operator")).isInstanceOf(AccessDeniedException.class);
    }
}
