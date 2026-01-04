package br.com.rentafit.people.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.mapper.PeopleMapper;
import br.com.rentafit.people.repository.CustomerRepository;
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
 * Testes unitários do CustomerService usando Mockito.
 * NÃO usa banco de dados (nem H2 nem PostgreSQL).
 * Testa apenas a lógica de negócio do serviço.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService - Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PeopleMapper peopleMapper;

    @InjectMocks
    private CustomerService customerService;

    private UUID customerId;
    private Customer customer;
    private CustomerDTO customerDTO;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();

        customer = new Customer();
        customer.setId(customerId);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setDocument("12345678900");

        customerDTO = CustomerDTO.builder()
                .id(customerId)
                .name("John Doe")
                .email("john@example.com")
                .document("12345678900")
                .build();
    }

    @Test
    @DisplayName("Deve encontrar todos os clientes com paginação")
    void shouldFindAllCustomersWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> customerPage = new PageImpl<>(List.of(customer));

        when(customerRepository.findAll(pageable)).thenReturn(customerPage);
        when(peopleMapper.toDTO(customer)).thenReturn(customerDTO);

        // Act
        Page<CustomerDTO> result = customerService.findAll(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("John Doe");

        verify(customerRepository, times(1)).findAll(pageable);
        verify(peopleMapper, times(1)).toDTO(customer);
    }

    @Test
    @DisplayName("Deve encontrar cliente por ID")
    void shouldFindCustomerById() {
        // Arrange
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(peopleMapper.toDTO(customer)).thenReturn(customerDTO);

        // Act
        CustomerDTO result = customerService.findById(customerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(customerId);
        assertThat(result.name()).isEqualTo("John Doe");
        assertThat(result.email()).isEqualTo("john@example.com");

        verify(customerRepository, times(1)).findById(customerId);
        verify(peopleMapper, times(1)).toDTO(customer);
    }

    @Test
    @DisplayName("Deve lançar exceção quando cliente não encontrado por ID")
    void shouldThrowExceptionWhenCustomerNotFoundById() {
        // Arrange
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.findById(customerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found with id");

        verify(customerRepository, times(1)).findById(customerId);
        verify(peopleMapper, never()).toDTO(any(Customer.class));
    }

    @Test
    @DisplayName("Deve criar novo cliente")
    void shouldCreateNewCustomer() {
        // Arrange
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(peopleMapper.toDTO(customer)).thenReturn(customerDTO);
        doNothing().when(peopleMapper).updateFromDTO(any(Customer.class), any(CustomerDTO.class));

        // Act
        CustomerDTO result = customerService.create(customerDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("John Doe");
        assertThat(result.email()).isEqualTo("john@example.com");

        verify(peopleMapper, times(1)).updateFromDTO(any(Customer.class), eq(customerDTO));
        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(peopleMapper, times(1)).toDTO(customer);
    }

    @Test
    @DisplayName("Deve atualizar cliente existente")
    void shouldUpdateExistingCustomer() {
        // Arrange
        CustomerDTO updatedDTO = CustomerDTO.builder()
                .id(customerId)
                .name("John Doe Updated")
                .email("john.updated@example.com")
                .document("12345678900")
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(peopleMapper.toDTO(customer)).thenReturn(updatedDTO);
        doNothing().when(peopleMapper).updateFromDTO(customer, updatedDTO);

        // Act
        CustomerDTO result = customerService.update(customerId, updatedDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("John Doe Updated");

        verify(customerRepository, times(1)).findById(customerId);
        verify(peopleMapper, times(1)).updateFromDTO(customer, updatedDTO);
        verify(customerRepository, times(1)).save(customer);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar cliente inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentCustomer() {
        // Arrange
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.update(customerId, customerDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found with id");

        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve deletar cliente por ID")
    void shouldDeleteCustomerById() {
        // Arrange
        when(customerRepository.existsById(customerId)).thenReturn(true);
        doNothing().when(customerRepository).deleteById(customerId);

        // Act
        customerService.delete(customerId);

        // Assert
        verify(customerRepository, times(1)).existsById(customerId);
        verify(customerRepository, times(1)).deleteById(customerId);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar cliente inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentCustomer() {
        // Arrange
        when(customerRepository.existsById(customerId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> customerService.delete(customerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found with id");

        verify(customerRepository, times(1)).existsById(customerId);
        verify(customerRepository, never()).deleteById(any());
    }
}
