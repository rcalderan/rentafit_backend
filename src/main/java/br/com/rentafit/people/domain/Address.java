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

    public Address(AddressDTO dto){
        this.zipCode = ZipCodeUtils.normalize(dto.zipCode());
        this.street = dto.street();
        this.neighborhood = dto.neighborhood();
        this.city = dto.city();
        this.state = dto.state();
    }

    public Address(ViaCepResponseDTO viaCepDTO){
        this.zipCode =  ZipCodeUtils.normalize(viaCepDTO.cep());
        this.street = viaCepDTO.logradouro();
        this.neighborhood = viaCepDTO.bairro();
        this.city = viaCepDTO.localidade();
        this.state = viaCepDTO.uf();
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
