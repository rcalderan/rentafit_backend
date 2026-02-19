package br.com.rentafit.people.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.domain.Address;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.domain.PersonAddressDetails;
import br.com.rentafit.people.domain.PersonAddressHistory;
import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.AddressHistoryDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.CustomerDetailsDTO;
import br.com.rentafit.people.mapper.PeopleMapper;
import br.com.rentafit.people.repository.CustomerRepository;
import br.com.rentafit.people.repository.PersonAddressDetailsRepository;
import br.com.rentafit.people.repository.PersonAddressHistoryRepository;
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
import org.springframework.web.client.HttpClientErrorException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de CustomerService.
 * Testa operações CRUD e fluxo de endereços com histórico para Cobertura JaCoCo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService - Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PersonAddressDetailsRepository addressDetailsRepository;

    @Mock
    private PersonAddressHistoryRepository addressHistoryRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private PeopleMapper peopleMapper;

    @InjectMocks
    private CustomerService customerService;

    private UUID customerId;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        testCustomer = new Customer();
        testCustomer.setId(customerId);
        testCustomer.setName("Test Customer");
        testCustomer.setEmail("test@example.com");
        testCustomer.setDocument("12345678900");
    }

    // ==================== findAll Tests ====================

    @Test
    @DisplayName("Deve listar clientes com paginação")
    void shouldFindAllCustomersWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> customerPage = new PageImpl<>(List.of(testCustomer), pageable, 1);

        when(customerRepository.findAll(pageable)).thenReturn(customerPage);

        // Act
        Page<CustomerDetailsDTO> result = customerService.findAll(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(customerRepository).findAll(pageable);
    }

    // ==================== findById Tests ====================

    @Test
    @DisplayName("Deve encontrar cliente por ID")
    void shouldFindCustomerById() {
        // Arrange
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));

        // Act
        CustomerDetailsDTO result = customerService.findById(customerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(customerId);
        verify(customerRepository).findById(customerId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando cliente não encontrado")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        UUID notFoundId = UUID.randomUUID();
        when(customerRepository.findById(notFoundId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.findById(notFoundId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== create Tests ====================

    @Test
    @DisplayName("Deve criar novo cliente sem endereço")
    void shouldCreateNewCustomerWithoutAddress() {
        // Arrange
        CustomerDTO createDTO = CustomerDTO.builder()
                .name("New Customer")
                .email("new@example.com")
                .document("99988877766")
                .build();

        Customer saved = new Customer();
        saved.setId(UUID.randomUUID());
        saved.setName("New Customer");
        saved.setEmail("new@example.com");
        saved.setDocument("99988877766");

        when(customerRepository.findByDocument(anyString())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        // Act
        CustomerDetailsDTO result = customerService.create(createDTO);

        // Assert
        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
        verify(addressService, never()).findOrCreateByAddress(any());
    }

    @Test
    @DisplayName("Deve criar novo cliente com endereço e details")
    void shouldCreateNewCustomerWithAddressAndDetails() {
        // Arrange
        AddressDTO addressDTO = AddressDTO.builder()
                .zipCode("01310-100")
                .street("Avenida Paulista")
                .city("São Paulo")
                .state("SP")
                .build();

        CustomerDTO createDTO = CustomerDTO.builder()
                .name("New Customer")
                .email("new@example.com")
                .document("99988877766")
                .number("1000")
                .complement("Apt 201")
                .address(addressDTO)
                .build();

        Address viaCepAddress = new Address("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP");
        when(customerRepository.findByDocument(anyString())).thenReturn(Optional.empty());
        when(addressService.findOrCreateByAddress(any(AddressDTO.class))).thenReturn(viaCepAddress);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CustomerDetailsDTO result = customerService.create(createDTO);

        // Assert
        assertThat(result).isNotNull();
        verify(addressService).findOrCreateByAddress(any(AddressDTO.class));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Deve rejeitar criação com documento duplicado")
    void shouldRejectCreateWithDuplicateDocument() {
        // Arrange
        Customer existing = new Customer();
        existing.setId(UUID.randomUUID());
        existing.setDocument("99988877766");
        when(customerRepository.findByDocument("99988877766")).thenReturn(Optional.of(existing));

        CustomerDTO createDTO = CustomerDTO.builder()
                .name("New Customer")
                .email("new@example.com")
                .document("99988877766")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> customerService.create(createDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists");

        verify(customerRepository, never()).save(any());
    }

    // ==================== update Tests ====================

    @Test
    @DisplayName("Deve atualizar endereço quando CEP muda e arquivar histórico")
    void shouldUpdateAddressWhenZipChangesAndArchiveHistory() {
        // Arrange - current details
        Address oldAddress = new Address("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP");
        PersonAddressDetails currentDetails = new PersonAddressDetails();
        currentDetails.setAddress(oldAddress);
        currentDetails.setNumber("1000");
        currentDetails.setComplement("Apt 201");
        currentDetails.setStartDate(OffsetDateTime.now().minusMonths(1));
        testCustomer.setCurrentAddress(currentDetails);

        Address newAddress = new Address("12345678", "Rua Nova", "Centro", "Rio de Janeiro", "RJ");
        AddressDTO addressDTO = AddressDTO.builder().zipCode("12345-678").street("Rua Nova").city("Rio de Janeiro").state("RJ").build();
        CustomerDTO updateDTO = CustomerDTO.builder()
                .id(customerId)
                .name("Updated")
                .email("u@example.com")
                .document("123")
                .number("2000")
                .complement("Apt 301")
                .address(addressDTO)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        doAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            PersonAddressDetails newDetails = new PersonAddressDetails();
            newDetails.setAddress(newAddress);
            newDetails.setNumber("2000");
            newDetails.setComplement("Apt 301");
            c.setCurrentAddress(newDetails);
            return null;
        }).when(addressService).handleAddressUpdate(any(Customer.class), any(CustomerDTO.class));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CustomerDetailsDTO result = customerService.update(updateDTO);

        // Assert
        assertThat(result).isNotNull();
        // Verify the service called the necessary methods to update address
        verify(addressService).handleAddressUpdate(any(Customer.class), any(CustomerDTO.class));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Deve atualizar apenas details quando número/complemento mudam")
    void shouldUpdateOnlyDetailsWhenNumberOrComplementChanges() {
        // Arrange current details same address
        Address address = new Address("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP");
        PersonAddressDetails currentDetails = new PersonAddressDetails();
        currentDetails.setAddress(address);
        currentDetails.setNumber("1000");
        currentDetails.setComplement("Apt 201");
        currentDetails.setStartDate(OffsetDateTime.now().minusMonths(1));
        testCustomer.setCurrentAddress(currentDetails);

        AddressDTO addressDTO = AddressDTO.builder().zipCode("01310-100").build();
        CustomerDTO updateDTO = CustomerDTO.builder()
                .id(customerId)
                .number("1001") // change number
                .complement("Apt 202")
                .address(addressDTO)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        doAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            PersonAddressDetails updated = c.getCurrentAddress();
            updated.setNumber("1001");
            updated.setComplement("Apt 202");
            return null;
        }).when(addressService).handleAddressUpdate(any(Customer.class), any(CustomerDTO.class));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CustomerDetailsDTO result = customerService.update(updateDTO);

        // Assert
        assertThat(result).isNotNull();
        // Verify update was processed (details changed even though address stayed same)
        verify(addressService).handleAddressUpdate(any(Customer.class), any(CustomerDTO.class));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Não deve arquivar quando nenhuma mudança ocorre")
    void shouldNotArchiveWhenNoChangeOccurs() {
        // Arrange current details
        Address address = new Address("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP");
        PersonAddressDetails currentDetails = new PersonAddressDetails();
        currentDetails.setAddress(address);
        currentDetails.setNumber("1000");
        currentDetails.setComplement("Apt 201");
        currentDetails.setStartDate(OffsetDateTime.now().minusMonths(1));
        testCustomer.setCurrentAddress(currentDetails);

        AddressDTO addressDTO = AddressDTO.builder().zipCode("01310-100").build();
        CustomerDTO updateDTO = CustomerDTO.builder()
                .id(customerId)
                .number("1000")
                .complement("Apt 201")
                .address(addressDTO)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CustomerDetailsDTO result = customerService.update(updateDTO);

        // Assert
        assertThat(result).isNotNull();
        verify(addressHistoryRepository, never()).save(any());
        verify(addressDetailsRepository, never()).save(any());
        verify(addressService, never()).findOrCreateByAddress(any());
    }

    // ==================== delete Tests ====================

    @Test
    @DisplayName("Deve deletar cliente existente")
    void shouldDeleteExistingCustomer() {
        // Arrange
        when(customerRepository.existsById(customerId)).thenReturn(true);

        // Act
        customerService.delete(customerId);

        // Assert
        verify(customerRepository).deleteById(customerId);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar cliente inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentCustomer() {
        // Arrange
        UUID notFoundId = UUID.randomUUID();
        when(customerRepository.existsById(notFoundId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> customerService.delete(notFoundId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== getAddressHistory Tests ====================

    @Test
    @DisplayName("Deve recuperar histórico de endereços do cliente")
    void shouldGetAddressHistory() {
        // Arrange
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(30);
        OffsetDateTime endDate = OffsetDateTime.now().minusDays(5);

        PersonAddressHistory history = new PersonAddressHistory();
        history.setId(UUID.randomUUID());
        history.setPersonId(customerId);
        history.setZipCode("01310100");
        history.setStreet("Avenida Paulista");
        history.setCity("São Paulo");
        history.setStartDate(startDate);
        history.setEndDate(endDate);

        AddressHistoryDTO historyDTO = AddressHistoryDTO.builder()
                .zipCode("01310-100")
                .street("Avenida Paulista")
                .build();

        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(addressHistoryRepository.findByPersonIdOrderByStartDateDesc(customerId))
                .thenReturn(List.of(history));
        when(peopleMapper.toHistoryDTO(history)).thenReturn(historyDTO);

        // Act
        List<AddressHistoryDTO> result = customerService.getAddressHistory(customerId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().zipCode()).isEqualTo("01310-100");
        verify(addressHistoryRepository).findByPersonIdOrderByStartDateDesc(customerId);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar histórico de cliente inexistente")
    void shouldThrowExceptionWhenGettingHistoryOfNonExistentCustomer() {
        // Arrange
        UUID notFoundId = UUID.randomUUID();
        when(customerRepository.existsById(notFoundId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> customerService.getAddressHistory(notFoundId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há histórico")
    void shouldReturnEmptyListWhenNoHistory() {
        // Arrange
        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(addressHistoryRepository.findByPersonIdOrderByStartDateDesc(customerId))
                .thenReturn(new ArrayList<>());

        // Act
        List<AddressHistoryDTO> result = customerService.getAddressHistory(customerId);

        // Assert
        assertThat(result).isEmpty();
    }
}

