package br.com.rentafit.people.web;

import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.service.CustomerService;
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
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do CustomerController usando Mockito.
 * Testa apenas a camada web (controllers) sem banco de dados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerController - Unit Tests")
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private UUID customerId;
    private CustomerDTO customerDTO;
    private AddressDTO addressDTO;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();

        addressDTO = AddressDTO.builder()
                .zipCode("12345-678")
                .street("Rua Teste")
                .neighborhood("Centro")
                .city("São Paulo")
                .state("SP")
                .build();

        customerDTO = CustomerDTO.builder()
                .id(customerId)
                .name("João Silva")
                .email("joao@example.com")
                .document("12345678900")
                .number("123")
                .complement("Apto 10")
                .address(addressDTO)
                .phones(List.of("11987654321"))
                .isAuthenticated(true)
                .build();
    }

    @Test
    @DisplayName("Should return paginated customers when findAll is called")
    void testFindAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<CustomerDTO> page = new PageImpl<>(List.of(customerDTO), pageable, 1);
        when(customerService.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        Page<CustomerDTO> result = customerController.findAll(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(customerId);
        assertThat(result.getContent().get(0).name()).isEqualTo("João Silva");
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(customerService, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should return customer when findById is called with valid ID")
    void testFindById() {
        // Arrange
        when(customerService.findById(customerId)).thenReturn(customerDTO);

        // Act
        CustomerDTO result = customerController.findById(customerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(customerId);
        assertThat(result.name()).isEqualTo("João Silva");
        assertThat(result.email()).isEqualTo("joao@example.com");
        assertThat(result.document()).isEqualTo("12345678900");

        verify(customerService, times(1)).findById(customerId);
    }

    @Test
    @DisplayName("Should create customer when valid data is provided")
    void testCreate() {
        // Arrange
        when(customerService.create(any(CustomerDTO.class))).thenReturn(customerDTO);

        // Act
        CustomerDTO result = customerController.create(customerDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(customerId);
        assertThat(result.name()).isEqualTo("João Silva");

        verify(customerService, times(1)).create(any(CustomerDTO.class));
    }

    @Test
    @DisplayName("Should update customer when valid data is provided")
    void testUpdate() {
        // Arrange
        CustomerDTO updatedDTO = CustomerDTO.builder()
                .id(customerId)
                .name("João Silva Updated")
                .email("joao.updated@example.com")
                .document("12345678900")
                .number("123")
                .complement("Apto 10")
                .address(addressDTO)
                .phones(List.of("11987654321"))
                .isAuthenticated(true)
                .build();

        when(customerService.update(eq(customerId), any(CustomerDTO.class))).thenReturn(updatedDTO);

        // Act
        CustomerDTO result = customerController.update(customerId, updatedDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(customerId);
        assertThat(result.name()).isEqualTo("João Silva Updated");
        assertThat(result.email()).isEqualTo("joao.updated@example.com");

        verify(customerService, times(1)).update(eq(customerId), any(CustomerDTO.class));
    }

    @Test
    @DisplayName("Should delete customer when valid ID is provided")
    void testDelete() {
        // Arrange
        doNothing().when(customerService).delete(customerId);

        // Act
        customerController.delete(customerId);

        // Assert
        verify(customerService, times(1)).delete(customerId);
    }
}

