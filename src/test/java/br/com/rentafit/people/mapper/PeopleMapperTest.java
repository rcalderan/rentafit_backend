package br.com.rentafit.people.mapper;

import br.com.rentafit.people.domain.Address;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.EmployeeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do PeopleMapper.
 * Testa as conversões Entity ↔ DTO.
 */
@DisplayName("PeopleMapper - Unit Tests")
class PeopleMapperTest {

    private PeopleMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PeopleMapper();
    }

    @Test
    @DisplayName("Deve converter Customer para CustomerDTO")
    void shouldConvertCustomerToDTO() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        Address address = new Address();
        address.setId(addressId);
        address.setStreet("Main Street");
        address.setCity("New York");
        address.setState("NY");
        address.setZipCode("10001");

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setDocument("12345678900");
        customer.setAddress(address);
        customer.setNumber("123");
        customer.setComplement("Apt 4B");
        customer.setPhones(List.of("123456789", "987654321"));
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
        assertThat(dto.number()).isEqualTo("123");
        assertThat(dto.complement()).isEqualTo("Apt 4B");
        assertThat(dto.address()).isNotNull();
        assertThat(dto.address().street()).isEqualTo("Main Street");
        assertThat(dto.address().city()).isEqualTo("New York");
        assertThat(dto.phones()).hasSize(2);
        assertThat(dto.phones()).contains("123456789", "987654321");
    }

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
    @DisplayName("Deve converter Address para AddressDTO")
    void shouldConvertAddressToDTO() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        Address address = new Address();
        address.setId(addressId);
        address.setStreet("Oak Avenue");
        address.setNeighborhood("Downtown");
        address.setCity("Los Angeles");
        address.setState("CA");
        address.setZipCode("90001");

        // Act
        AddressDTO dto = mapper.toDTO(address);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(addressId);
        assertThat(dto.street()).isEqualTo("Oak Avenue");
        assertThat(dto.neighborhood()).isEqualTo("Downtown");
        assertThat(dto.city()).isEqualTo("Los Angeles");
        assertThat(dto.state()).isEqualTo("CA");
        assertThat(dto.zipCode()).isEqualTo("90001");
    }

    @Test
    @DisplayName("Deve atualizar Customer a partir de CustomerDTO")
    void shouldUpdateCustomerFromDTO() {
        // Arrange
        Customer customer = new Customer();

        AddressDTO addressDTO = AddressDTO.builder()
                .street("New Street")
                .city("Boston")
                .state("MA")
                .zipCode("02101")
                .build();

        CustomerDTO dto = CustomerDTO.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .document("99988877766")
                .isAuthenticated(true)
                .notes("Updated notes")
                .number("999")
                .complement("Suite 10")
                .address(addressDTO)
                .phones(new ArrayList<>(List.of("111222333", "444555666")))
                .build();

        // Act
        mapper.updateFromDTO(customer, dto);

        // Assert
        assertThat(customer.getName()).isEqualTo("Updated Name");
        assertThat(customer.getEmail()).isEqualTo("updated@example.com");
        assertThat(customer.getDocument()).isEqualTo("99988877766");
        assertThat(customer.getIsAuthenticated()).isTrue();
        assertThat(customer.getNotes()).isEqualTo("Updated notes");
        assertThat(customer.getNumber()).isEqualTo("999");
        assertThat(customer.getComplement()).isEqualTo("Suite 10");
        assertThat(customer.getPhones()).hasSize(2);
        assertThat(customer.getAddress()).isNotNull();
        assertThat(customer.getAddress().getStreet()).isEqualTo("New Street");
        assertThat(customer.getAddress().getCity()).isEqualTo("Boston");
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

    @Test
    @DisplayName("Deve atualizar Address a partir de AddressDTO")
    void shouldUpdateAddressFromDTO() {
        // Arrange
        Address address = new Address();

        AddressDTO dto = AddressDTO.builder()
                .street("Updated Street")
                .neighborhood("New Neighborhood")
                .city("Chicago")
                .state("IL")
                .zipCode("60601")
                .build();

        // Act
        mapper.updateFromDTO(address, dto);

        // Assert
        assertThat(address.getStreet()).isEqualTo("Updated Street");
        assertThat(address.getNeighborhood()).isEqualTo("New Neighborhood");
        assertThat(address.getCity()).isEqualTo("Chicago");
        assertThat(address.getState()).isEqualTo("IL");
        assertThat(address.getZipCode()).isEqualTo("60601");
    }

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

    @Test
    @DisplayName("Deve lidar com Customer sem endereço")
    void shouldHandleCustomerWithoutAddress() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("John Doe");
        customer.setAddress(null);

        // Act
        CustomerDTO dto = mapper.toDTO(customer);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(customerId);
        assertThat(dto.address()).isNull();
    }

    @Test
    @DisplayName("Deve criar novo Address quando DTO tem endereço mas entity não")
    void shouldCreateNewAddressWhenDTOHasAddressButEntityDoesNot() {
        // Arrange
        Customer customer = new Customer();
        customer.setAddress(null);

        AddressDTO addressDTO = AddressDTO.builder()
                .street("New Street")
                .city("Miami")
                .state("FL")
                .zipCode("33101")
                .build();

        CustomerDTO dto = CustomerDTO.builder()
                .name("John")
                .address(addressDTO)
                .build();

        // Act
        mapper.updateFromDTO(customer, dto);

        // Assert
        assertThat(customer.getAddress()).isNotNull();
        assertThat(customer.getAddress().getStreet()).isEqualTo("New Street");
        assertThat(customer.getAddress().getCity()).isEqualTo("Miami");
    }
}
