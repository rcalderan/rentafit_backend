package br.com.rentafit.people.domain;

import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.dto.ViaCepResponseDTO;
import br.com.rentafit.people.util.ZipCodeUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Immutable address entity.
 * Addresses are shared across multiple people to avoid duplication.
 */
@Entity
@Table(name = "addresses", uniqueConstraints = {
    @UniqueConstraint(name = "uk_address_composition", columnNames = {"zip_code", "street", "city", "state"})
})
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor
@Schema(description = "Immutable address entity")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private final UUID id;

    @Column(name = "zip_code", length = 8)
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

    @Column(name = "is_manual", nullable = false)
    private final boolean isManual;

    public Address(AddressDTO dto){
        this(dto, false);
    }

    public Address(AddressDTO dto, boolean isManual){
        this.id = null;
        this.zipCode = dto.zipCode() != null ? ZipCodeUtils.normalize(dto.zipCode()) : null;
        this.street = dto.street();
        this.neighborhood = dto.neighborhood();
        this.city = dto.city();
        this.state = dto.state();
        this.isManual = isManual;
    }

    public Address(ViaCepResponseDTO viaCepDTO){
        this.id = null;
        this.zipCode =  ZipCodeUtils.normalize(viaCepDTO.cep());
        this.street = viaCepDTO.logradouro() != null ? viaCepDTO.logradouro() : "";
        this.neighborhood = viaCepDTO.bairro();
        this.city = viaCepDTO.localidade() != null ? viaCepDTO.localidade() : "";
        this.state = viaCepDTO.uf() != null ? viaCepDTO.uf() : "";
        this.isManual = false;
    }

    public Address(String zipCode, String street, String neighborhood, String city, String state) {
        this.id = null;
        this.zipCode = zipCode != null ? ZipCodeUtils.normalize(zipCode) : null;
        this.street = street;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.isManual = false;
    }

    public AddressDTO toDTO() {
        return AddressDTO.builder()
                .zipCode(ZipCodeUtils.format(this.zipCode))
                .street(this.street)
                .neighborhood(this.neighborhood)
                .city(this.city)
                .state(this.state)
                .build();
    }
}
