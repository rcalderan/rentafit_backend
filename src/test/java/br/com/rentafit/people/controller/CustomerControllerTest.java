package br.com.rentafit.people.controller;

import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.CustomerDetailsDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        CustomerDetailsDTO detailsDTO = new CustomerDetailsDTO(customerId, null, "João Silva", "12345678900",
                "joao@example.com", true, "VIP", addressDTO, "123", "Apto 10", List.of());
        Page<CustomerDetailsDTO> page = new PageImpl<>(List.of(detailsDTO), pageable, 1);
        when(customerService.search(null, pageable)).thenReturn(page);

        // Act
        ResponseEntity<Page<CustomerDetailsDTO>> response = customerController.findAll(null, pageable);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().getFirst().id()).isEqualTo(customerId);
        assertThat(response.getBody().getContent().getFirst().name()).isEqualTo("João Silva");
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);

        verify(customerService, times(1)).search(null, pageable);
    }

    @Test
    @DisplayName("Should return paginated customers when findByNamePrefix is called")
    void testFindByNamePrefix() {
        Pageable pageable = PageRequest.of(0, 10);
        CustomerDetailsDTO detailsDTO = new CustomerDetailsDTO(customerId, null, "João Silva", "12345678900",
                "joao@example.com", true, "VIP", addressDTO, "123", "Apto 10", List.of());
        Page<CustomerDetailsDTO> page = new PageImpl<>(List.of(detailsDTO), pageable, 1);
        when(customerService.findByNamePrefix("Jo", pageable)).thenReturn(page);

        ResponseEntity<Page<CustomerDetailsDTO>> response = customerController.findByNamePrefix("Jo", pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(customerService, times(1)).findByNamePrefix("Jo", pageable);
    }

    @Test
    @DisplayName("Should return customer when findById is called with valid ID")
    void testFindById() {
        // Arrange
        CustomerDetailsDTO detailsDTO = new CustomerDetailsDTO(customerId, null, "João Silva", "12345678900",
                "joao@example.com", true, "VIP", addressDTO, "123", "Apto 10", List.of());
        when(customerService.findById(customerId)).thenReturn(detailsDTO);

        // Act
        ResponseEntity<CustomerDetailsDTO> response = customerController.findById(customerId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(customerId);
        assertThat(response.getBody().name()).isEqualTo("João Silva");
        assertThat(response.getBody().email()).isEqualTo("joao@example.com");
        assertThat(response.getBody().document()).isEqualTo("12345678900");

        verify(customerService, times(1)).findById(customerId);
    }

    @Test
    @DisplayName("Should create customer when valid data is provided")
    void testCreate() {
        // Arrange
        CustomerDetailsDTO detailsDTO = new CustomerDetailsDTO(customerId, null, "João Silva", "12345678900",
                "joao@example.com", true, "VIP", addressDTO, "123", "Apto 10", List.of("11987654321"));
        when(customerService.create(any(CustomerDTO.class))).thenReturn(detailsDTO);

        // Act
        ResponseEntity<CustomerDetailsDTO> response = customerController.create(customerDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(customerId);
        assertThat(response.getBody().name()).isEqualTo("João Silva");

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

        CustomerDetailsDTO detailsDTO = new CustomerDetailsDTO(customerId, null, "João Silva Updated", "12345678900",
                "joao.updated@example.com", true, null, addressDTO, "123", "Apto 10", List.of("11987654321"));
        when(customerService.update(any(CustomerDTO.class))).thenReturn(detailsDTO);

        // Act
        ResponseEntity<CustomerDetailsDTO> response = customerController.update(updatedDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(customerId);
        assertThat(response.getBody().name()).isEqualTo("João Silva Updated");
        assertThat(response.getBody().email()).isEqualTo("joao.updated@example.com");

        verify(customerService, times(1)).update(any(CustomerDTO.class));
    }

    @Test
    @DisplayName("Should delete customer when valid ID is provided")
    void testDelete() {
        // Arrange
        doNothing().when(customerService).delete(customerId);

        // Act
        ResponseEntity<Void> response = customerController.delete(customerId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(customerService, times(1)).delete(customerId);
    }

    @Test
    @DisplayName("Should return customer when findByDocument is called with valid document")
    void testFindByDocument() {
        // Arrange
        String document = "12345678900";
        CustomerDetailsDTO detailsDTO = new CustomerDetailsDTO(customerId, null, "João Silva", document,
                "joao@example.com", true, "VIP", addressDTO, "123", "Apto 10", List.of());
        when(customerService.findByDocument(document)).thenReturn(detailsDTO);

        // Act
        ResponseEntity<CustomerDetailsDTO> response = customerController.findByDocument(document);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().document()).isEqualTo(document);
        assertThat(response.getBody().name()).isEqualTo("João Silva");

        verify(customerService, times(1)).findByDocument(document);
    }

    @Test
    @DisplayName("Should return customer when findByLegacyId is called with valid legacy ID")
    void testFindByLegacyId() {
        // Arrange
        Integer legacyId = 123;
        CustomerDetailsDTO detailsDTO = new CustomerDetailsDTO(customerId, legacyId, "João Silva", "12345678900",
                "joao@example.com", true, "VIP", addressDTO, "123", "Apto 10", List.of());
        when(customerService.findByLegacyId(legacyId)).thenReturn(detailsDTO);

        // Act
        ResponseEntity<CustomerDetailsDTO> response = customerController.findByLegacyId(legacyId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().legacyId()).isEqualTo(legacyId);
        assertThat(response.getBody().name()).isEqualTo("João Silva");

        verify(customerService, times(1)).findByLegacyId(legacyId);
    }

    @Test
    @DisplayName("Should return address history when getAddressHistory is called")
    void testGetAddressHistory() {
        // Arrange
        var history1 = new br.com.rentafit.people.dto.AddressHistoryDTO(
                UUID.randomUUID(),
                "01310-100",
                "Avenida Paulista",
                "Bela Vista",
                "São Paulo",
                "SP",
                "123",
                "Apto 10",
                java.time.OffsetDateTime.now().minusMonths(6),
                java.time.OffsetDateTime.now().minusMonths(1),
                java.time.OffsetDateTime.now().minusMonths(1),
                false
        );
        var history2 = new br.com.rentafit.people.dto.AddressHistoryDTO(
                UUID.randomUUID(),
                "12345-678",
                "Rua Antiga",
                "Centro",
                "Rio de Janeiro",
                "RJ",
                "456",
                "Casa",
                java.time.OffsetDateTime.now().minusYears(2),
                java.time.OffsetDateTime.now().minusMonths(6),
                java.time.OffsetDateTime.now().minusMonths(6),
                false
        );
        List<br.com.rentafit.people.dto.AddressHistoryDTO> historyList = List.of(history1, history2);

        when(customerService.getAddressHistory(customerId)).thenReturn(historyList);

        // Act
        ResponseEntity<List<br.com.rentafit.people.dto.AddressHistoryDTO>> response =
                customerController.getAddressHistory(customerId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).number()).isEqualTo("123");
        assertThat(response.getBody().get(1).number()).isEqualTo("456");

        verify(customerService, times(1)).getAddressHistory(customerId);
    }
}

