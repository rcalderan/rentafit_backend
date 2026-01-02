package br.com.rentafit.people.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standalone address entity")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Unique identifier (UUID)")
    private UUID id;

    @Column(name = "zip_code")
    @Schema(description = "ZIP/Postal code", example = "01234-567")
    private String zipCode;

    @Schema(description = "Street name", example = "Main Street")
    private String street;

    @Schema(description = "Neighborhood", example = "Downtown")
    private String neighborhood;

    @Schema(description = "City", example = "São Paulo")
    private String city;

    @Column(length = 2)
    @Schema(description = "State/Province code", example = "SP")
    private String state;
}
