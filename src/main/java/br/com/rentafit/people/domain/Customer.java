package br.com.rentafit.people.domain;

import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.CustomerDetailsDTO;
import br.com.rentafit.people.util.ZipCodeUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@NamedEntityGraph(
    name = "Customer.withAddress",
    attributeNodes = {
        @NamedAttributeNode(value = "addressDetails", subgraph = "address-subgraph")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "address-subgraph",
            attributeNodes = @NamedAttributeNode("address")
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Customer entity extending Person")
public class Customer extends Person {

    @Column(name = "is_authenticated")
    @Schema(description = "Whether the customer is authenticated", example = "false")
    @Getter
    @Setter
    private Boolean isAuthenticated = false;

    @Schema(description = "General notes about the customer")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    @Schema(description = "Employee who created this customer record")
    private Employee createdBy;

    @ElementCollection
    @CollectionTable(name = "customer_phones", joinColumns = @JoinColumn(name = "customer_id"))
    @Column(name = "phone")
    @Schema(description = "List of customer phone numbers")
    private List<String> phones = new ArrayList<>();

    public Customer(CustomerDTO dto){
        if (dto == null) return;
        this.setName(dto.name());
        this.setDocument(dto.document());
        this.setEmail(dto.email());
        this.setIsAuthenticated(dto.isAuthenticated());
        this.setNotes(dto.notes());
        this.setPhones(dto.phones() != null ? new ArrayList<>(dto.phones()) : new ArrayList<>());
    }

    /**
     * Update customer basic fields from DTO
     * Note: Address update is handled separately in CustomerService
     */
    public Customer updateBasicFieldsFromDTO(CustomerDTO dto) {
        if (dto == null) return this;
        this.setName(dto.name());
        this.setDocument(dto.document());
        this.setEmail(dto.email());
        this.setIsAuthenticated(dto.isAuthenticated());
        this.setNotes(dto.notes());
        this.setPhones(dto.phones() != null ? new ArrayList<>(dto.phones()) : new ArrayList<>());
        return this;
    }


    public CustomerDetailsDTO toDTO() {

        AddressDTO addressDTO = null;
        String number = null;
        String complement = null;

        if (this.getCurrentAddress() != null) {
            PersonAddressDetails details = this.getCurrentAddress();
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

        return CustomerDetailsDTO.builder()
                .id(this.getId())
                .name(this.getName())
                .document(this.getDocument())
                .email(this.getEmail())
                .isAuthenticated(this.getIsAuthenticated() != null && this.getIsAuthenticated())
                .notes(this.getNotes())
                .number(number)
                .complement(complement)
                .address(addressDTO)
                .phones(this.getPhones())
                .build();
    }

}
