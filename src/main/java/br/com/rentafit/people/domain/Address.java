package br.com.rentafit.people.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Immutable;

/**
 * Immutable address entity using ZIP code as primary key.
 * Addresses are shared across multiple people to avoid duplication.
 */
@Entity
@Table(name = "addresses")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor
@Schema(description = "Immutable address entity indexed by ZIP code")
public class Address {

    @Id
    @Column(name = "zip_code", nullable = false, unique = true, length = 8)
    @Schema(description = "ZIP/Postal code (8 digits, no hyphen)", example = "01234567")
    private final String zipCode;

    @Column(nullable = false)
    @Schema(description = "Street name", example = "Rua das Flores")
    private final String street;

    @Schema(description = "Neighborhood", example = "Centro")
    private final String neighborhood;

    @Column(nullable = false)
    @Schema(description = "City", example = "São Paulo")
    private final String city;

    @Column(nullable = false, length = 2)
    @Schema(description = "State/Province code", example = "SP")
    private final String state;
}
