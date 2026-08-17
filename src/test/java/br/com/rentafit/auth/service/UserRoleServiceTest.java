package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.UserSummaryDTO;
import br.com.rentafit.auth.repository.RoleRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRoleService")
class UserRoleServiceTest {

    @Mock
    UserAccountRepository userAccountRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    EmployeeService employeeService;

    @InjectMocks
    UserRoleService service;

    UserAccount admin;
    UserAccount target;
    UUID targetId;

    @BeforeEach
    void setUp() {
        admin = new UserAccount();
        admin.setId(UUID.randomUUID());
        admin.setUsername("admin@test.com");

        targetId = UUID.randomUUID();
        target = new UserAccount();
        target.setId(targetId);
        target.setUsername("user@test.com");
    }

    @Test
    @DisplayName("setRole lança ValidationException quando actor é null")
    void setRole_actorNull() {
        assertThatThrownBy(() -> service.setRole(null, targetId, RoleName.EMPLOYEE))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("setRole lança ValidationException quando actor tenta mudar a própria role")
    void setRole_selfChange() {
        assertThatThrownBy(() -> service.setRole(admin, admin.getId(), RoleName.EMPLOYEE))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("setRole lança ResourceNotFoundException quando target não existe")
    void setRole_targetNotFound() {
        admin.setRoles(List.of(roleWithName(RoleName.ADMIN)));
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setRole(admin, targetId, RoleName.EMPLOYEE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("setRole com ADMIN muda role com sucesso e garante Employee ao elevar para EMPLOYEE")
    void setRole_adminSuccess_elevatingToEmployee() {
        admin.setRoles(List.of(roleWithName(RoleName.ADMIN)));
        target.setRoles(List.of(roleWithName(RoleName.CUSTOMER)));

        Role employeeRole = roleWithName(RoleName.EMPLOYEE);
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findByRole(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(userAccountRepository.save(any())).thenReturn(target);

        service.setRole(admin, targetId, RoleName.EMPLOYEE, "JD", 1);

        verify(employeeService).ensureEmployeeForPerson(eq(targetId), eq("JD"), eq(1));
    }

    @Test
    @DisplayName("setRole para MANAGER também garante Employee row")
    void setRole_elevatingToManager_ensuresEmployee() {
        admin.setRoles(List.of(roleWithName(RoleName.ADMIN)));
        target.setRoles(List.of(roleWithName(RoleName.CUSTOMER)));

        Role managerRole = roleWithName(RoleName.MANAGER);
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findByRole(RoleName.MANAGER)).thenReturn(Optional.of(managerRole));
        when(userAccountRepository.save(any())).thenReturn(target);

        service.setRole(admin, targetId, RoleName.MANAGER, "JD", null);

        verify(employeeService).ensureEmployeeForPerson(eq(targetId), eq("JD"), eq(null));
    }

    @Test
    @DisplayName("setRole para CUSTOMER não chama ensureEmployeeForPerson")
    void setRole_demotingToCustomer_skipsEmployeeEnsure() {
        admin.setRoles(List.of(roleWithName(RoleName.ADMIN)));
        target.setRoles(List.of(roleWithName(RoleName.EMPLOYEE)));

        Role customerRole = roleWithName(RoleName.CUSTOMER);
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findByRole(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userAccountRepository.save(any())).thenReturn(target);

        service.setRole(admin, targetId, RoleName.CUSTOMER);

        verify(employeeService, never()).ensureEmployeeForPerson(any(), any(), any());
    }

    @Test
    @DisplayName("setRole propaga ValidationException do EmployeeService quando initials já em uso")
    void setRole_initialsConflict_propagatesValidationException() {
        admin.setRoles(List.of(roleWithName(RoleName.ADMIN)));
        target.setRoles(List.of(roleWithName(RoleName.CUSTOMER)));
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(target));
        doThrow(new ValidationException("Initials 'JD' already in use"))
                .when(employeeService).ensureEmployeeForPerson(eq(targetId), eq("JD"), eq(1));

        assertThatThrownBy(() -> service.setRole(admin, targetId, RoleName.EMPLOYEE, "JD", 1))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Initials 'JD' already in use");
    }

    @Test
    @DisplayName("setRole lança ValidationException quando EmployeeService exige initials e elas vêm null")
    void setRole_initialsRequiredWhenMissing() {
        admin.setRoles(List.of(roleWithName(RoleName.ADMIN)));
        target.setRoles(List.of(roleWithName(RoleName.CUSTOMER)));
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(target));
        doThrow(new ValidationException(
                "Initials are required to register Employee for person " + targetId))
                .when(employeeService).ensureEmployeeForPerson(eq(targetId), eq(null), eq(null));

        assertThatThrownBy(() -> service.setRole(admin, targetId, RoleName.EMPLOYEE, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Initials are required");
    }

    @Test
    @DisplayName("setRole com MANAGER lança ValidationException ao tentar atribuir ADMIN")
    void setRole_managerCannotAssignAdmin() {
        admin.setRoles(List.of(roleWithName(RoleName.MANAGER)));
        target.setRoles(List.of(roleWithName(RoleName.CUSTOMER)));
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.setRole(admin, targetId, RoleName.ADMIN))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("MANAGER cannot assign role ADMIN");
    }

    @Test
    @DisplayName("setRole com MANAGER lança ValidationException quando target tem role MANAGER")
    void setRole_managerCannotModifyManager() {
        admin.setRoles(List.of(roleWithName(RoleName.MANAGER)));
        target.setRoles(List.of(roleWithName(RoleName.MANAGER)));
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.setRole(admin, targetId, RoleName.EMPLOYEE))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("setRole lança ValidationException quando actor não tem permissão suficiente")
    void setRole_insufficientPermission() {
        admin.setRoles(List.of(roleWithName(RoleName.EMPLOYEE)));
        target.setRoles(List.of(roleWithName(RoleName.CUSTOMER)));
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.setRole(admin, targetId, RoleName.EMPLOYEE))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("setRole lança ValidationException quando role configurada não existe no repositório")
    void setRole_roleNotConfigured() {
        admin.setRoles(List.of(roleWithName(RoleName.ADMIN)));
        target.setRoles(List.of(roleWithName(RoleName.CUSTOMER)));
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(target));
        // ensureEmployeeForPerson is a void method; default Mockito does nothing (treated as success).
        when(roleRepository.findByRole(RoleName.EMPLOYEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setRole(admin, targetId, RoleName.EMPLOYEE, "JD", 1))
                .isInstanceOf(ValidationException.class);
    }

    private Role roleWithName(RoleName name) {
        Role role = new Role();
        role.setRole(name);
        return role;
    }
}
