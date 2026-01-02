package br.com.rentafit.people.mapper;

import br.com.rentafit.people.domain.Address;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.EmployeeDTO;
import org.springframework.stereotype.Component;

@Component
public class PeopleMapper {

    public CustomerDTO toDTO(Customer customer) {
        if (customer == null) return null;

        return CustomerDTO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .document(customer.getDocument())
                .email(customer.getEmail())
                .isAuthenticated(customer.getIsAuthenticated() != null && customer.getIsAuthenticated())
                .notes(customer.getNotes())
                .number(customer.getNumber())
                .complement(customer.getComplement())
                .address(toDTO(customer.getAddress()))
                .phones(customer.getPhones())
                .build();
    }

    public AddressDTO toDTO(Address address) {
        if (address == null) return null;

        return AddressDTO.builder()
                .id(address.getId())
                .zipCode(address.getZipCode())
                .street(address.getStreet())
                .neighborhood(address.getNeighborhood())
                .city(address.getCity())
                .state(address.getState())
                .build();
    }

    public EmployeeDTO toDTO(Employee employee) {
        if (employee == null) return null;

        return EmployeeDTO.builder()
                .id(employee.getId())
                .name(employee.getName())
                .document(employee.getDocument())
                .email(employee.getEmail())
                .initials(employee.getInitials())
                .roleLevel(employee.getRoleLevel())
                .build();
    }

    public void updateFromDTO(Employee employee, EmployeeDTO dto) {
        if (employee == null || dto == null) return;

        employee.setName(dto.name());
        employee.setDocument(dto.document());
        employee.setEmail(dto.email());
        employee.setInitials(dto.initials());
        employee.setRoleLevel(dto.roleLevel());
    }

    public void updateFromDTO(Customer customer, CustomerDTO dto) {
        if (customer == null || dto == null) return;

        customer.setName(dto.name());
        customer.setDocument(dto.document());
        customer.setEmail(dto.email());
        customer.setIsAuthenticated(dto.isAuthenticated());
        customer.setNotes(dto.notes());
        customer.setNumber(dto.number());
        customer.setComplement(dto.complement());
        customer.setPhones(dto.phones() != null ? new java.util.ArrayList<>(dto.phones()) : new java.util.ArrayList<>());

        if (dto.address() != null) {
            Address address = customer.getAddress();
            if (address == null) {
                address = new Address();
            }
            updateFromDTO(address, dto.address());
            customer.setAddress(address);
        }
    }

    public void updateFromDTO(Address address, AddressDTO dto) {
        if (address == null || dto == null) return;

        address.setZipCode(dto.zipCode());
        address.setStreet(dto.street());
        address.setNeighborhood(dto.neighborhood());
        address.setCity(dto.city());
        address.setState(dto.state());
    }
}
