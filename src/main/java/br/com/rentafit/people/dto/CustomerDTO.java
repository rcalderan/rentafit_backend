package br.com.rentafit.people.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.util.List;
import java.util.UUID;

@Builder
public record CustomerDTO(
    UUID id,

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @Size(max = 50, message = "Document must not exceed 50 characters")
    @Pattern(regexp = "^[0-9]{11}$|^[0-9]{14}$", message = "Document must be a valid CPF (11 digits) or CNPJ (14 digits)")
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

    @NotNull(message = "A lista de telefones não pode ser nula")
    @Size(min = 1, max = 5, message = "O cliente deve ter entre 1 e 5 telefones")
    List<String> phones

//    @NotNull(message = "A lista de telefones não pode ser nula")
//    @Size(min = 1, max = 2, message = "O cliente deve ter entre 1 e 2 telefones")
//    List<@NotBlank(message = "O número do telefone não pode estar em branco")
//    @Size(max = 20, message = "O telefone não deve exceder 20 caracteres") String> phones
) {}
