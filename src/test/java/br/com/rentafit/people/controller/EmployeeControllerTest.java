package br.com.rentafit.people.controller;

import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.people.dto.ActiveAttendantDTO;
import br.com.rentafit.people.dto.EmployeeDTO;
import br.com.rentafit.people.dto.EmployeeAuthResponseDTO;
import br.com.rentafit.people.dto.EmployeeCheckRequestDTO;
import br.com.rentafit.people.dto.EmployeeCheckResponseDTO;
import br.com.rentafit.people.dto.EmployeeCreateRequestDTO;
import br.com.rentafit.people.service.EmployeeService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do EmployeeController usando Mockito.
 * Testa apenas a camada web (controllers) sem banco de dados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeController - Unit Tests")
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    private UUID employeeId;
    private EmployeeDTO employeeDTO;
    private EmployeeCheckResponseDTO employeeCheckResponseDTO;
    private EmployeeCreateRequestDTO employeeCreateRequestDTO;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();

        employeeDTO = EmployeeDTO.builder()
                .id(employeeId)
                .name("Maria Santos")
                .email("maria@example.com")
                .document("98765432100")
                .initials("MS")
                .roleLevel(2)
                .build();

        employeeCheckResponseDTO = EmployeeCheckResponseDTO.builder()
                .id(employeeId)
                .name("Maria Santos")
                .initials("MS")
                .build();

        employeeCreateRequestDTO = new EmployeeCreateRequestDTO(
                "Maria Santos",
                "98765432100",
                "maria@example.com",
                "ms",
                2,
                "1234"
        );
    }

    @Test
    @DisplayName("Should return paginated employees when findAll is called")
    void testFindAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<EmployeeDTO> page = new PageImpl<>(List.of(employeeDTO), pageable, 1);
        when(employeeService.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        ResponseEntity<Page<EmployeeDTO>> response = employeeController.findAll(pageable);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).id()).isEqualTo(employeeId);
        assertThat(response.getBody().getContent().get(0).name()).isEqualTo("Maria Santos");
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);

        verify(employeeService, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should return active attendants")
    void testFindActiveAttendants() {
        ActiveAttendantDTO attendant = new ActiveAttendantDTO(employeeId, "Maria Santos", RoleName.MANAGER);
        when(employeeService.findActiveAttendants()).thenReturn(List.of(attendant));

        ResponseEntity<List<ActiveAttendantDTO>> response = employeeController.findActiveAttendants();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(attendant);
        verify(employeeService).findActiveAttendants();
    }

    @Test
    @DisplayName("Should return employee when findById is called with valid ID")
    void testFindById() {
        // Arrange
        when(employeeService.findById(employeeId)).thenReturn(employeeDTO);

        // Act
        ResponseEntity<EmployeeDTO> response = employeeController.findById(employeeId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(employeeId);
        assertThat(response.getBody().name()).isEqualTo("Maria Santos");
        assertThat(response.getBody().email()).isEqualTo("maria@example.com");
        assertThat(response.getBody().document()).isEqualTo("98765432100");
        assertThat(response.getBody().initials()).isEqualTo("MS");
        assertThat(response.getBody().roleLevel()).isEqualTo(2);

        verify(employeeService, times(1)).findById(employeeId);
    }

    @Test
    @DisplayName("Should return employee when findByInitials is called with valid initials")
    void testFindByInitials() {
        when(employeeService.findByInitials("MS")).thenReturn(employeeCheckResponseDTO);

        ResponseEntity<EmployeeCheckResponseDTO> response = employeeController.findByInitials("MS");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(employeeId);
        assertThat(response.getBody().initials()).isEqualTo("MS");

        verify(employeeService, times(1)).findByInitials("MS");
    }

    @Test
    @DisplayName("Should create employee when valid data is provided")
    void testCreate() {
        when(employeeService.create(any(EmployeeCreateRequestDTO.class))).thenReturn(employeeCheckResponseDTO);

        ResponseEntity<EmployeeCheckResponseDTO> response = employeeController.create(employeeCreateRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(employeeId);
        assertThat(response.getBody().name()).isEqualTo("Maria Santos");
        assertThat(response.getBody().initials()).isEqualTo("MS");

        verify(employeeService, times(1)).create(any(EmployeeCreateRequestDTO.class));
    }

    @Test
    @DisplayName("Should check employee credentials when valid initials and pin are provided")
    void testCheck() {
        String initials = "MS";
        String pin = "1234";
        String name = "Jao";
        EmployeeCheckRequestDTO requestDTO = new EmployeeCheckRequestDTO(initials, pin);
        when(employeeService.check(requestDTO)).thenReturn(new EmployeeCheckResponseDTO(employeeId, initials, name));

        ResponseEntity<EmployeeCheckResponseDTO> response = employeeController.check(requestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(employeeId);

        verify(employeeService, times(1)).check(requestDTO);
    }

    @Test
    @DisplayName("Should update employee when valid data is provided")
    void testUpdate() {
        // Arrange
        EmployeeDTO updatedDTO = EmployeeDTO.builder()
                .id(employeeId)
                .name("Maria Santos Updated")
                .email("maria.updated@example.com")
                .document("98765432100")
                .initials("MSU")
                .roleLevel(3)
                .build();

        when(employeeService.update(eq(employeeId), any(EmployeeDTO.class))).thenReturn(updatedDTO);

        // Act
        ResponseEntity<EmployeeDTO> response = employeeController.update(employeeId, updatedDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(employeeId);
        assertThat(response.getBody().name()).isEqualTo("Maria Santos Updated");
        assertThat(response.getBody().email()).isEqualTo("maria.updated@example.com");
        assertThat(response.getBody().initials()).isEqualTo("MSU");
        assertThat(response.getBody().roleLevel()).isEqualTo(3);

        verify(employeeService, times(1)).update(eq(employeeId), any(EmployeeDTO.class));
    }

    @Test
    @DisplayName("Should delete employee when valid ID is provided")
    void testDelete() {
        // Arrange
        doNothing().when(employeeService).delete(employeeId);

        // Act
        ResponseEntity<Void> response = employeeController.delete(employeeId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(employeeService, times(1)).delete(employeeId);
    }
}

