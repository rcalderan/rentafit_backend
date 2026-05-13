package br.com.rentafit.people.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeCheckRequestDTO(
        @NotBlank(message = "Initials are required")
        @Size(max = 10, message = "Initials must not exceed 10 characters")
        String initials,

        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 numeric digits")
        String pin
) {
}

