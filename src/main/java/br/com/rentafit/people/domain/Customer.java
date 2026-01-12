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
}
