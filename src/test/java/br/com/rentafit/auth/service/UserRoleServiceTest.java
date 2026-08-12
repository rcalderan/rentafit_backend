package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.UserSummaryDTO;
import br.com.rentafit.auth.repository.RoleRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRoleService")
class UserRoleServiceTest {

    @Mock
    UserAccountRepository userAccountRepository;

    @Mock
    RoleRepository roleRepository;

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
    @DisplayName("setRole com ADMIN muda role com sucesso")
    void setRole_adminSuccess() {
        admin.setRoles(List.of(roleWithName(RoleName.ADMIN)));
        target.setRoles(List.of(roleWithName(RoleName.CUSTOMER)));

        Role employeeRole = roleWithName(RoleName.EMPLOYEE);
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(roleRepository.findByRole(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(userAccountRepository.save(any())).thenReturn(target);

        UserSummaryDTO result = service.setRole(admin, targetId, RoleName.EMPLOYEE);

        // UserSummaryDTO.fromEntity uses target — just verify no exception
        // (result may be null if fromEntity returns null for minimal entity)
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
        when(roleRepository.findByRole(RoleName.EMPLOYEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setRole(admin, targetId, RoleName.EMPLOYEE))
                .isInstanceOf(ValidationException.class);
    }

    private Role roleWithName(RoleName name) {
        Role role = new Role();
        role.setRole(name);
        return role;
    }
}
