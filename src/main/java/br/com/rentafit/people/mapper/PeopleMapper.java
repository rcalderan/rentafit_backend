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

}
