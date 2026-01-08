package br.com.rentafit.people.domain;

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
import java.util.UUID;

@Entity
@Table(name = "customers")
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

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id")
    @Schema(description = "Reference to the base address")
    private Address address;

    @Schema(description = "Specific number for the address", example = "123")
    private String number;

    @Schema(description = "Address complement", example = "Apt 4B")
    private String complement;

    @ElementCollection
    @CollectionTable(name = "customer_phones", joinColumns = @JoinColumn(name = "customer_id"))
    @Column(name = "phone")
    @Schema(description = "List of customer phone numbers")
    private List<String> phones = new ArrayList<>();
}
