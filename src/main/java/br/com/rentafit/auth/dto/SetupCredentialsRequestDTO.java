package br.com.rentafit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request for first-access credential setup (password + PIN).
 * Password: min 8 chars, ≥1 uppercase, ≥1 digit, ≥1 special char.
 * PIN: exactly 4 numeric digits.
 */
public record SetupCredentialsRequestDTO(

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#+]).{8,}$",
                message = "Password must contain at least 1 uppercase letter, 1 number, and 1 special character (@$!%*?&#+)"
        )
        String newPassword,

        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 numeric digits")
        String pin
) {
}
