package br.com.rentafit.people.dto;

import br.com.rentafit.common.validation.ValidCpfCnpj;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;
import java.util.UUID;

@Builder
public record CustomerDetailsDTO(
    @NotNull
    UUID id,

    @Size(message = "LegacyId integer")
    Integer legacyId,

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @ValidCpfCnpj
    String document,

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    boolean isAuthenticated,

    String notes,

    @Valid
    AddressDTO address,

    @Size(max = 20, message = "Number must not exceed 20 characters")
    String number,

    @Size(max = 100, message = "Complement must not exceed 100 characters")
    String complement,

    @UniqueElements(message = "Os números de telefone devem ser únicos")
    @NotNull(message = "A lista de telefones não pode ser nula")
    @Size(min = 1, max = 5, message = "O cliente deve ter entre 1 e 5 telefones")
    List<String> phones

) {}
