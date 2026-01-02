package br.com.rentafit.people.service;

import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.dto.EmployeeDTO;
import br.com.rentafit.people.mapper.PeopleMapper;
import br.com.rentafit.people.repository.EmployeeRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private PeopleMapper peopleMapper;

    @InjectMocks
    private EmployeeService employeeService;

    private UUID employeeId;
    private Employee employee;
    private EmployeeDTO employeeDTO;

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
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Employee not found with id");

        verify(employeeRepository, times(1)).findById(employeeId);
        verify(peopleMapper, never()).toDTO(any(Employee.class));
    }

    @Test
    @DisplayName("Deve criar novo funcionário")
    void shouldCreateNewEmployee() {
        // Arrange
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(peopleMapper.toDTO(employee)).thenReturn(employeeDTO);
        doNothing().when(peopleMapper).updateFromDTO(any(Employee.class), any(EmployeeDTO.class));

        // Act
        EmployeeDTO result = employeeService.create(employeeDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Jane Smith");
        assertThat(result.initials()).isEqualTo("JS");

        verify(peopleMapper, times(1)).updateFromDTO(any(Employee.class), eq(employeeDTO));
        verify(employeeRepository, times(1)).save(any(Employee.class));
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
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Employee not found with id");

        verify(employeeRepository, times(1)).existsById(employeeId);
        verify(employeeRepository, never()).deleteById(any());
    }
}

