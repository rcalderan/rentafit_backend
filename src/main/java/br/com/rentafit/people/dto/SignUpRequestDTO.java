package br.com.rentafit.people.dto;

import br.com.rentafit.common.validation.ValidCpfCnpj;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

/**
 * Public self-registration payload. Creates a Customer + UserAccount (CUSTOMER role).
 * No password is collected here: the account is created with a random placeholder
 * password and a null PIN, and the user finishes setup at /auth/setup-credentials.
 */
@Builder
@Schema(description = "Public self-registration request")
public record SignUpRequestDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Document is required")
        @ValidCpfCnpj
        String document,

        @NotNull(message = "A lista de telefones não pode ser nula")
        @Size(min = 1, max = 5, message = "O cliente deve ter entre 1 e 5 telefones")
        List<String> phones,

        @NotNull(message = "Address is required")
        @Valid
        AddressDTO address,

        @Size(max = 20, message = "Number must not exceed 20 characters")
        String number,

        @Size(max = 100, message = "Complement must not exceed 100 characters")
        String complement
) {}
