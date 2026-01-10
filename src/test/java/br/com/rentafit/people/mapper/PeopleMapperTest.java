package br.com.rentafit.people.mapper;

import br.com.rentafit.people.domain.Address;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.domain.PersonAddressDetails;
import br.com.rentafit.people.domain.PersonAddressHistory;
import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.AddressHistoryDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.EmployeeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do PeopleMapper.
 * Testa as conversões Entity ↔ DTO com nova arquitetura de endereços.
 */
@DisplayName("PeopleMapper - Unit Tests")
class PeopleMapperTest {

    private PeopleMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PeopleMapper();
    }

    // ==================== Customer Mapping Tests ====================

    @Test
    @DisplayName("Deve converter Customer com endereço para CustomerDTO")
    void shouldConvertCustomerWithAddressToDTO() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID personAddressDetailsId = UUID.randomUUID();

        Address address = new Address(
                "01310100",
                "Avenida Paulista",
                "Bela Vista",
                "São Paulo",
                "SP"
        );

        PersonAddressDetails addressDetails = new PersonAddressDetails();
        addressDetails.setId(personAddressDetailsId);
        addressDetails.setAddress(address);
        addressDetails.setNumber("1000");
        addressDetails.setComplement("Apt 201");
        addressDetails.setStartDate(OffsetDateTime.now());
        addressDetails.setEndDate(null);

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setDocument("12345678900");
        customer.setCurrentAddress(addressDetails);
        customer.setPhones(List.of("1123456789", "1187654321"));
        customer.setIsAuthenticated(true);
        customer.setNotes("VIP Customer");

        // Act
        CustomerDTO dto = mapper.toDTO(customer);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(customerId);
        assertThat(dto.name()).isEqualTo("John Doe");
        assertThat(dto.email()).isEqualTo("john@example.com");
        assertThat(dto.document()).isEqualTo("12345678900");
        assertThat(dto.isAuthenticated()).isTrue();
        assertThat(dto.notes()).isEqualTo("VIP Customer");
        assertThat(dto.number()).isEqualTo("1000");
        assertThat(dto.complement()).isEqualTo("Apt 201");
        assertThat(dto.address()).isNotNull();
        assertThat(dto.address().zipCode()).isEqualTo("01310-100"); // Formatado com hífen
        assertThat(dto.address().street()).isEqualTo("Avenida Paulista");
        assertThat(dto.address().neighborhood()).isEqualTo("Bela Vista");
        assertThat(dto.phones()).hasSize(2);
        assertThat(dto.phones()).contains("1123456789", "1187654321");
    }

    @Test
    @DisplayName("Deve converter Customer sem endereço para CustomerDTO")
    void shouldConvertCustomerWithoutAddressToDTO() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("Jane Doe");
        customer.setEmail("jane@example.com");
        customer.setCurrentAddress(null);

        // Act
        CustomerDTO dto = mapper.toDTO(customer);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(customerId);
        assertThat(dto.name()).isEqualTo("Jane Doe");
        assertThat(dto.address()).isNull();
        assertThat(dto.number()).isNull();
        assertThat(dto.complement()).isNull();
    }

    @Test
    @DisplayName("Deve atualizar Customer fields básicos a partir de CustomerDTO")
    void shouldUpdateBasicFieldsFromDTO() {
        // Arrange
        Customer customer = new Customer();

        CustomerDTO dto = CustomerDTO.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .document("99988877766")
                .isAuthenticated(true)
                .notes("Updated notes")
                .phones(new ArrayList<>(List.of("111222333", "444555666")))
                .build();

        // Act
        mapper.updateBasicFields(customer, dto);

        // Assert
        assertThat(customer.getName()).isEqualTo("Updated Name");
        assertThat(customer.getEmail()).isEqualTo("updated@example.com");
        assertThat(customer.getDocument()).isEqualTo("99988877766");
        assertThat(customer.getIsAuthenticated()).isTrue();
        assertThat(customer.getNotes()).isEqualTo("Updated notes");
        assertThat(customer.getPhones()).hasSize(2);
    }

    @Test
    @DisplayName("Deve atualizar Customer com null phones")
    void shouldUpdateCustomerWithNullPhones() {
        // Arrange
        Customer customer = new Customer();

        CustomerDTO dto = CustomerDTO.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .document("99988877766")
                .isAuthenticated(false)
                .phones(null)
                .build();

        // Act
        mapper.updateBasicFields(customer, dto);

        // Assert
        assertThat(customer.getPhones()).isEmpty();
    }

    // ==================== Employee Mapping Tests ====================

    @Test
    @DisplayName("Deve converter Employee para EmployeeDTO")
    void shouldConvertEmployeeToDTO() {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setName("Jane Smith");
        employee.setEmail("jane@example.com");
        employee.setDocument("98765432100");
        employee.setInitials("JS");
        employee.setRoleLevel(2);

        // Act
        EmployeeDTO dto = mapper.toDTO(employee);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(employeeId);
        assertThat(dto.name()).isEqualTo("Jane Smith");
        assertThat(dto.email()).isEqualTo("jane@example.com");
        assertThat(dto.document()).isEqualTo("98765432100");
        assertThat(dto.initials()).isEqualTo("JS");
        assertThat(dto.roleLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve atualizar Employee a partir de EmployeeDTO")
    void shouldUpdateEmployeeFromDTO() {
        // Arrange
        Employee employee = new Employee();

        EmployeeDTO dto = EmployeeDTO.builder()
                .name("Updated Employee")
                .email("updated.employee@example.com")
                .document("11122233344")
                .initials("UE")
                .roleLevel(3)
                .build();

        // Act
        mapper.updateFromDTO(employee, dto);

        // Assert
        assertThat(employee.getName()).isEqualTo("Updated Employee");
        assertThat(employee.getEmail()).isEqualTo("updated.employee@example.com");
        assertThat(employee.getDocument()).isEqualTo("11122233344");
        assertThat(employee.getInitials()).isEqualTo("UE");
        assertThat(employee.getRoleLevel()).isEqualTo(3);
    }

    // ==================== Address Mapping Tests ====================

    @Test
    @DisplayName("Deve converter Address para AddressDTO com formatação de zipCode")
    void shouldConvertAddressToDTO() {
        // Arrange
        Address address = new Address(
                "01310100",
                "Avenida Paulista",
                "Bela Vista",
                "São Paulo",
                "SP"
        );

        // Act
        AddressDTO dto = mapper.toDTO(address);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.zipCode()).isEqualTo("01310-100"); // Formatado com hífen
        assertThat(dto.street()).isEqualTo("Avenida Paulista");
        assertThat(dto.neighborhood()).isEqualTo("Bela Vista");
        assertThat(dto.city()).isEqualTo("São Paulo");
        assertThat(dto.state()).isEqualTo("SP");
    }

    // ==================== AddressHistory Mapping Tests ====================

    @Test
    @DisplayName("Deve converter PersonAddressHistory para AddressHistoryDTO")
    void shouldConvertAddressHistoryToDTO() {
        // Arrange
        UUID historyId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(30);
        OffsetDateTime endDate = OffsetDateTime.now().minusDays(5);
        OffsetDateTime archivedAt = OffsetDateTime.now();

        PersonAddressHistory history = new PersonAddressHistory();
        history.setId(historyId);
        history.setPersonId(personId);
        history.setZipCode("01310100");
        history.setStreet("Avenida Paulista");
        history.setNeighborhood("Bela Vista");
        history.setCity("São Paulo");
        history.setState("SP");
        history.setNumber("1000");
        history.setComplement("Apt 201");
        history.setStartDate(startDate);
        history.setEndDate(endDate);
        history.setArchivedAt(archivedAt);

        // Act
        AddressHistoryDTO dto = mapper.toHistoryDTO(history);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(historyId);
        assertThat(dto.zipCode()).isEqualTo("01310-100");
        assertThat(dto.street()).isEqualTo("Avenida Paulista");
        assertThat(dto.neighborhood()).isEqualTo("Bela Vista");
        assertThat(dto.city()).isEqualTo("São Paulo");
        assertThat(dto.state()).isEqualTo("SP");
        assertThat(dto.number()).isEqualTo("1000");
        assertThat(dto.complement()).isEqualTo("Apt 201");
        assertThat(dto.startDate()).isEqualTo(startDate);
        assertThat(dto.endDate()).isEqualTo(endDate);
        assertThat(dto.archivedAt()).isEqualTo(archivedAt);
    }

    @Test
    @DisplayName("Deve retornar null ao converter null AddressHistory")
    void shouldReturnNullForNullAddressHistory() {
        // Act
        AddressHistoryDTO dto = mapper.toHistoryDTO(null);

        // Assert
        assertThat(dto).isNull();
    }

    // ==================== Null Handling Tests ====================

    @Test
    @DisplayName("Deve lidar com valores null graciosamente")
    void shouldHandleNullValuesGracefully() {
        // Act
        CustomerDTO customerDTO = mapper.toDTO((Customer) null);
        EmployeeDTO employeeDTO = mapper.toDTO((Employee) null);
        AddressDTO addressDTO = mapper.toDTO((Address) null);

        // Assert
        assertThat(customerDTO).isNull();
        assertThat(employeeDTO).isNull();
        assertThat(addressDTO).isNull();
    }

    // ==================== Helper Methods Tests ====================

    @Test
    @DisplayName("Deve retornar null ao chamar updateFromDTO com null customer")
    void shouldHandleNullCustomerInUpdate() {
        // Arrange
        CustomerDTO dto = CustomerDTO.builder()
                .name("Test")
                .build();

        // Act & Assert
        mapper.updateBasicFields(null, dto); // Should not throw
    }

    @Test
    @DisplayName("Deve retornar null ao chamar updateFromDTO com null dto")
    void shouldHandleNullDTOInUpdate() {
        // Arrange
        Customer customer = new Customer();

        // Act & Assert
        mapper.updateBasicFields(customer, null); // Should not throw
    }

    @Test
    @DisplayName("Deve retornar null ao chamar updateFromDTO employee com null employee")
    void shouldHandleNullEmployeeInUpdate() {
        // Arrange
        EmployeeDTO dto = EmployeeDTO.builder()
                .name("Test")
                .build();

        // Act & Assert
        mapper.updateFromDTO(null, dto); // Should not throw
    }

    @Test
    @DisplayName("Deve retornar null ao chamar updateFromDTO employee com null dto")
    void shouldHandleNullDTOEmployeeInUpdate() {
        // Arrange
        Employee employee = new Employee();

        // Act & Assert
        mapper.updateFromDTO(employee, null); // Should not throw
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("Deve converter Customer com todos os campos preenchidos")
    void shouldConvertFullyPopulatedCustomer() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID addressDetailsId = UUID.randomUUID();

        Address address = new Address(
                "12345678",
                "Main Street",
                "Downtown",
                "Test City",
                "TC"
        );

        PersonAddressDetails details = new PersonAddressDetails();
        details.setId(addressDetailsId);
        details.setAddress(address);
        details.setNumber("999");
        details.setComplement("Suite 100");
        details.setStartDate(OffsetDateTime.now());

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("Full Customer");
        customer.setEmail("full@example.com");
        customer.setDocument("12345678901");
        customer.setCurrentAddress(details);
        customer.setPhones(List.of("111111111", "222222222", "333333333"));
        customer.setIsAuthenticated(true);
        customer.setNotes("Full details");

        // Act
        CustomerDTO dto = mapper.toDTO(customer);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(customerId);
        assertThat(dto.number()).isEqualTo("999");
        assertThat(dto.complement()).isEqualTo("Suite 100");
        assertThat(dto.phones()).hasSize(3);
        assertThat(dto.address()).isNotNull();
        assertThat(dto.address().zipCode()).isEqualTo("12345-678");
    }

    @Test
    @DisplayName("Deve converter Customer com PersonAddressDetails sem complemento")
    void shouldConvertCustomerWithoutComplement() {
        // Arrange
        Address address = new Address(
                "87654321",
                "Main Ave",
                "Uptown",
                "City",
                "CI"
        );

        PersonAddressDetails details = new PersonAddressDetails();
        details.setAddress(address);
        details.setNumber("456");
        details.setComplement(null);
        details.setStartDate(OffsetDateTime.now());

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Test");
        customer.setCurrentAddress(details);

        // Act
        CustomerDTO dto = mapper.toDTO(customer);

        // Assert
        assertThat(dto.number()).isEqualTo("456");
        assertThat(dto.complement()).isNull();
    }

    @Test
    @DisplayName("Deve converter Address com vizinhança null")
    void shouldConvertAddressWithNullNeighborhood() {
        // Arrange
        Address address = new Address(
                "11111111",
                "Some Street",
                null,
                "Some City",
                "SC"
        );

        // Act
        AddressDTO dto = mapper.toDTO(address);

        // Assert
        assertThat(dto.neighborhood()).isNull();
        assertThat(dto.street()).isEqualTo("Some Street");
    }

    @Test
    @DisplayName("Deve converter Customer com empty phones list")
    void shouldConvertCustomerWithEmptyPhones() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("No Phones");
        customer.setPhones(new ArrayList<>());

        // Act
        CustomerDTO dto = mapper.toDTO(customer);

        // Assert
        assertThat(dto.phones()).isEmpty();
    }

    @Test
    @DisplayName("Deve converter Customer com PersonAddressDetails mas sem Address")
    void shouldConvertCustomerWithDetailsButNoAddress() {
        // Arrange
        PersonAddressDetails details = new PersonAddressDetails();
        details.setAddress(null);
        details.setNumber("123");

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Test");
        customer.setCurrentAddress(details);

        // Act
        CustomerDTO dto = mapper.toDTO(customer);

        // Assert
        assertThat(dto.address()).isNull();
        // When address is null in details, number still comes from details
        assertThat(dto.number()).isEqualTo("123");
    }

    @Test
    @DisplayName("Deve converter Employee com null document")
    void shouldConvertEmployeeWithNullDocument() {
        // Arrange
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setName("John");
        employee.setDocument(null);

        // Act
        EmployeeDTO dto = mapper.toDTO(employee);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.document()).isNull();
    }
}

