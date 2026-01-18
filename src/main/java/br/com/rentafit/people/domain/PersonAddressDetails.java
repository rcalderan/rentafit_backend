package br.com.rentafit.people.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Association entity linking a Person to an Address with specific details.
 * Contains person-specific address information like number and complement.
 */
@Entity
@Table(name = "person_address_details", indexes = {
    @Index(name = "idx_person_address_person_id", columnList = "person_id"),
    @Index(name = "idx_person_address_end_date", columnList = "end_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Person-specific address details with number and complement")
public class PersonAddressDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Unique identifier")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    @Schema(description = "Person associated with this address")
    private Person person;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "address_id", nullable = false)
    @Schema(description = "Address reference")
    private Address address;

    @Column(length = 20)
    @Schema(description = "Address number", example = "123")
    private String number;

    @Column(length = 100)
    @Schema(description = "Address complement", example = "Apt 4B")
    private String complement;

    @CreationTimestamp
    @Column(name = "start_date", nullable = false)
    @Schema(description = "When this address became active for the person")
    private OffsetDateTime startDate;

    @Column(name = "end_date")
    @Schema(description = "When this address stopped being active (null = current)")
    private OffsetDateTime endDate;

    /**
     * Check if this is the current/active address
     */
    public boolean isCurrent() {
        return endDate == null;
    }
}
