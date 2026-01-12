package br.com.rentafit.people.mapper;

import br.com.rentafit.people.domain.*;
import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.AddressHistoryDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.EmployeeDTO;
import br.com.rentafit.people.util.ZipCodeUtils;
import org.springframework.stereotype.Component;

@Component
public class PeopleMapper {

    public CustomerDTO toDTO(Customer customer) {
        if (customer == null) return null;

        AddressDTO addressDTO = null;
        String number = null;
        String complement = null;

        if (customer.getCurrentAddress() != null) {
            PersonAddressDetails details = customer.getCurrentAddress();
            if (details.getAddress() != null) {
                Address addr = details.getAddress();
                addressDTO = AddressDTO.builder()
                        .zipCode(ZipCodeUtils.format(addr.getZipCode()))
                        .street(addr.getStreet())
                        .neighborhood(addr.getNeighborhood())
                        .city(addr.getCity())
                        .state(addr.getState())
                        .build();
            }
            number = details.getNumber();
            complement = details.getComplement();
        }

        return CustomerDTO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .document(customer.getDocument())
                .email(customer.getEmail())
                .isAuthenticated(customer.getIsAuthenticated() != null && customer.getIsAuthenticated())
                .notes(customer.getNotes())
                .number(number)
                .complement(complement)
                .address(addressDTO)
                .phones(customer.getPhones())
                .build();
    }

    public AddressDTO toDTO(Address address) {
        if (address == null) return null;

        return AddressDTO.builder()
                .zipCode(ZipCodeUtils.format(address.getZipCode()))
                .street(address.getStreet())
                .neighborhood(address.getNeighborhood())
                .city(address.getCity())
                .state(address.getState())
                .build();
    }

    public AddressHistoryDTO toHistoryDTO(PersonAddressHistory history) {
        if (history == null) return null;

        return AddressHistoryDTO.builder()
                .id(history.getId())
                .zipCode(ZipCodeUtils.format(history.getZipCode()))
                .street(history.getStreet())
                .neighborhood(history.getNeighborhood())
                .city(history.getCity())
                .state(history.getState())
                .number(history.getNumber())
                .complement(history.getComplement())
                .startDate(history.getStartDate())
                .endDate(history.getEndDate())
                .archivedAt(history.getArchivedAt())
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

    /**
     * Update customer basic fields from DTO
     * Note: Address update is handled separately in CustomerService
     */
    public void updateBasicFields(Customer customer, CustomerDTO dto) {
        if (customer == null || dto == null) return;

        customer.setName(dto.name());
        customer.setDocument(dto.document());
        customer.setEmail(dto.email());
        customer.setIsAuthenticated(dto.isAuthenticated());
        customer.setNotes(dto.notes());
        customer.setPhones(dto.phones() != null ? new java.util.ArrayList<>(dto.phones()) : new java.util.ArrayList<>());
    }
}
