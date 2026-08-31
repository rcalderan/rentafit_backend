package br.com.rentafit.people.service;

import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.RoleRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.dto.ActiveAttendantDTO;
import br.com.rentafit.people.dto.EmployeeAuthResponseDTO;
import br.com.rentafit.people.dto.EmployeeCheckRequestDTO;
import br.com.rentafit.people.dto.EmployeeCheckResponseDTO;
import br.com.rentafit.people.dto.EmployeeCreateRequestDTO;
import br.com.rentafit.people.dto.EmployeeDTO;
import br.com.rentafit.people.mapper.PeopleMapper;
import br.com.rentafit.people.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do EmployeeService usando Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService - Unit Tests")
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EntityManager entityManager;

    @Mock
    private PeopleMapper peopleMapper;

    @InjectMocks
    private EmployeeService employeeService;

    private UUID employeeId;
    private Employee employee;
    private EmployeeDTO employeeDTO;
    private EmployeeCreateRequestDTO employeeCreateRequestDTO;
    private Role employeeRole;
    private Query nativeQueryMock;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();

        employee = new Employee();
        employee.setId(employeeId);
        employee.setName("Jane Smith");
        employee.setEmail("jane@example.com");
        employee.setInitials("JS");
        employee.setRoleLevel(2);

        employeeDTO = EmployeeDTO.builder()
                .id(employeeId)
                .name("Jane Smith")
                .email("jane@example.com")
                .initials("JS")
                .roleLevel(2)
                .build();

        employeeCreateRequestDTO = new EmployeeCreateRequestDTO(
                "Jane Smith",
                "98765432100",
                "jane@example.com",
                "js",
                2,
                "1234"
        );

        employeeRole = new Role();
        employeeRole.setId(3L);
        employeeRole.setRole(RoleName.EMPLOYEE);

        nativeQueryMock = mock(Query.class);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQueryMock);
        lenient().when(nativeQueryMock.setParameter(anyString(), any())).thenReturn(nativeQueryMock);
        lenient().when(nativeQueryMock.executeUpdate()).thenReturn(1);
    }

    @Test
    @DisplayName("Deve encontrar todos os funcionários com paginação")
    void shouldFindAllEmployeesWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Employee> employeePage = new PageImpl<>(List.of(employee));

        when(employeeRepository.findAll(pageable)).thenReturn(employeePage);
        when(peopleMapper.toDTO(employee)).thenReturn(employeeDTO);

        // Act
        Page<EmployeeDTO> result = employeeService.findAll(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Jane Smith");

        verify(employeeRepository, times(1)).findAll(pageable);
        verify(peopleMapper, times(1)).toDTO(employee);
    }

    @Test
    @DisplayName("Deve listar usuários ativos aptos a atender locações")
    void shouldFindActiveAttendants() {
        UserAccount account = new UserAccount();
        account.setId(employeeId);
        account.setPerson(employee);
        account.setRoles(List.of(employeeRole));

        when(userAccountRepository.findActiveAttendants(Set.of(
                RoleName.EMPLOYEE, RoleName.MANAGER, RoleName.ADMIN))).thenReturn(List.of(account));

        List<ActiveAttendantDTO> result = employeeService.findActiveAttendants();

        assertThat(result).containsExactly(
                new ActiveAttendantDTO(employeeId, "Jane Smith", RoleName.EMPLOYEE));
    }

    @Test
    @DisplayName("Deve encontrar funcionário por ID")
    void shouldFindEmployeeById() {
        // Arrange
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(peopleMapper.toDTO(employee)).thenReturn(employeeDTO);

        // Act
        EmployeeDTO result = employeeService.findById(employeeId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(employeeId);
        assertThat(result.name()).isEqualTo("Jane Smith");
        assertThat(result.initials()).isEqualTo("JS");

        verify(employeeRepository, times(1)).findById(employeeId);
        verify(peopleMapper, times(1)).toDTO(employee);
    }

    @Test
    @DisplayName("Deve lançar exceção quando funcionário não encontrado")
    void shouldThrowExceptionWhenEmployeeNotFound() {
        // Arrange
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.findById(employeeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id");

        verify(employeeRepository, times(1)).findById(employeeId);
        verify(peopleMapper, never()).toDTO(any(Employee.class));
    }

    @Test
    @DisplayName("Deve criar novo funcionário")
    void shouldCreateNewEmployee() {
        when(employeeRepository.saveAndFlush(any(Employee.class))).thenReturn(employee);
        when(roleRepository.findByRole(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-password");

        EmployeeCheckResponseDTO result = employeeService.create(employeeCreateRequestDTO);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Jane Smith");
        assertThat(result.initials()).isEqualTo("JS");

        verify(employeeRepository, times(1)).saveAndFlush(any(Employee.class));
        verify(roleRepository, times(1)).findByRole(RoleName.EMPLOYEE);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar quando role EMPLOYEE não existir")
    void shouldThrowExceptionWhenEmployeeRoleIsMissing() {
        when(employeeRepository.saveAndFlush(any(Employee.class))).thenReturn(employee);
        when(roleRepository.findByRole(RoleName.EMPLOYEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.create(employeeCreateRequestDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("EMPLOYEE role not configured");
    }

    @Test
    @DisplayName("Deve atualizar funcionário existente")
    void shouldUpdateExistingEmployee() {
        // Arrange
        EmployeeDTO updatedDTO = EmployeeDTO.builder()
                .id(employeeId)
                .name("Jane Smith Updated")
                .email("jane.updated@example.com")
                .initials("JS")
                .roleLevel(3)
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(peopleMapper.toDTO(employee)).thenReturn(updatedDTO);
        doNothing().when(peopleMapper).updateFromDTO(employee, updatedDTO);

        // Act
        EmployeeDTO result = employeeService.update(employeeId, updatedDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Jane Smith Updated");
        assertThat(result.roleLevel()).isEqualTo(3);

        verify(employeeRepository, times(1)).findById(employeeId);
        verify(peopleMapper, times(1)).updateFromDTO(employee, updatedDTO);
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    @DisplayName("Deve buscar funcionário por iniciais")
    void shouldFindEmployeeByInitials() {
        when(employeeRepository.findByInitials("JS")).thenReturn(Optional.of(employee));

        EmployeeCheckResponseDTO result = employeeService.findByInitials("js");

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(employeeId);
        assertThat(result.initials()).isEqualTo("JS");
    }

    @Test
    @DisplayName("Deve validar iniciais e PIN corretamente")
    void shouldCheckEmployeeCredentials() {
        UserAccount account = new UserAccount();
        account.setId(employeeId);
        account.setPin("$2a$10$hashedPin");
        account.setRoles(Collections.singletonList(employeeRole));

        when(employeeRepository.findByInitials("JS")).thenReturn(Optional.of(employee));
        when(userAccountRepository.findById(employeeId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("1234", "$2a$10$hashedPin")).thenReturn(true);

        EmployeeCheckRequestDTO requestDTO = new EmployeeCheckRequestDTO("js", "1234");
        EmployeeCheckResponseDTO result = employeeService.check(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(employeeId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando PIN for inválido")
    void shouldThrowExceptionWhenPinIsInvalid() {
        UserAccount account = new UserAccount();
        account.setId(employeeId);
        account.setPin("$2a$10$otherHashedPin");

        when(employeeRepository.findByInitials("JS")).thenReturn(Optional.of(employee));
        when(userAccountRepository.findById(employeeId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("1234", "$2a$10$otherHashedPin")).thenReturn(false);

        EmployeeCheckRequestDTO requestDTO = new EmployeeCheckRequestDTO("js", "1234");

        assertThatThrownBy(() -> employeeService.check(requestDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Credenciais inválidas");
    }

    @Test
    @DisplayName("Deve deletar funcionário por ID")
    void shouldDeleteEmployeeById() {
        // Arrange
        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(employeeId);

        // Act
        employeeService.delete(employeeId);

        // Assert
        verify(employeeRepository, times(1)).existsById(employeeId);
        verify(employeeRepository, times(1)).deleteById(employeeId);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar funcionário inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentEmployee() {
        // Arrange
        when(employeeRepository.existsById(employeeId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.delete(employeeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id");

        verify(employeeRepository, times(1)).existsById(employeeId);
        verify(employeeRepository, never()).deleteById(any());
    }
}

