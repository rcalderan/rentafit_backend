package br.com.rentafit.people.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import java.util.UUID;

@Builder
public record EmployeeDTO(
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

    @NotBlank(message = "Initials are required")
    @Size(max = 10, message = "Initials must not exceed 10 characters")
    String initials,

    @Min(value = 1, message = "Role level must be at least 1")
    @Max(value = 10, message = "Role level must not exceed 10")
    Integer roleLevel
) {}
