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
 * Historical record of person addresses for audit purposes.
 * Stores immutable snapshots of past addresses.
 */
@Entity
@Table(name = "person_address_history", indexes = {
    @Index(name = "idx_address_history_person_id", columnList = "person_id"),
    @Index(name = "idx_address_history_start_date", columnList = "start_date DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Historical address records for audit trail")
public class PersonAddressHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Unique identifier")
    private UUID id;

    @Column(name = "person_id", nullable = false)
    @Schema(description = "Person ID reference")
    private UUID personId;

    @Column(name = "zip_code", nullable = false, length = 8)
    @Schema(description = "ZIP code snapshot")
    private String zipCode;

    @Column(length = 255)
    @Schema(description = "Street name snapshot")
    private String street;

    @Column(length = 100)
    @Schema(description = "Neighborhood snapshot")
    private String neighborhood;

    @Column(length = 100)
    @Schema(description = "City snapshot")
    private String city;

    @Column(length = 2)
    @Schema(description = "State snapshot")
    private String state;

    @Column(length = 20)
    @Schema(description = "Address number")
    private String number;

    @Column(length = 100)
    @Schema(description = "Address complement")
    private String complement;

    @Column(name = "start_date", nullable = false)
    @Schema(description = "When this address became active")
    private OffsetDateTime startDate;

    @Column(name = "end_date", nullable = false)
    @Schema(description = "When this address stopped being active")
    private OffsetDateTime endDate;

    @CreationTimestamp
    @Column(name = "archived_at", nullable = false)
    @Schema(description = "When this record was archived")
    private OffsetDateTime archivedAt;
}

