package br.com.rentafit.people.domain;

//import br.com.rentafit.common.security.DatabaseEncryptionConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "people")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Base entity for all people in the system")
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Unique identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Column(name = "legacy_id", unique = true)
    @Schema(description = "ID from the legacy MongoDB system", example = "101")
    private Integer legacyId;

    @NotBlank
    @Column(nullable = false)
    @Schema(description = "Full name", example = "John Doe")
    private String name;

    @Getter
    @Column(unique = true)
    //@Convert(converter = DatabaseEncryptionConverter.class)
    @Schema(description = "Document (CPF/RG/CNPJ)", example = "123.456.789-00")
    private String document;

    @Email
    @Column(unique = true)
    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "Address history and current details")
    private List<PersonAddressDetails> addressDetails = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Creation timestamp with timezone")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Last update timestamp with timezone")
    private OffsetDateTime updatedAt;

    /**
     * Helper method to get the current active address details
     */
    public PersonAddressDetails getCurrentAddress() {
        return addressDetails.stream()
                .filter(PersonAddressDetails::isCurrent)
                .findFirst()
                .orElse(null);
    }

    /**
     * Helper method to set the current address details
     */
    public void setCurrentAddress(PersonAddressDetails details) {
        if (details != null) {
            details.setPerson(this);
            this.addressDetails.add(details);
        }
    }

    /**
     * Helper method to get the base address from current address details
     */
    public Address getAddress() {
        PersonAddressDetails current = getCurrentAddress();
        return current != null ? current.getAddress() : null;
    }

    /**
     * Helper method to get address number
     */
    public String getAddressNumber() {
        PersonAddressDetails current = getCurrentAddress();
        return current != null ? current.getNumber() : null;
    }

    /**
     * Helper method to get address complement
     */
    public String getAddressComplement() {
        PersonAddressDetails current = getCurrentAddress();
        return current != null ? current.getComplement() : null;
    }
}
