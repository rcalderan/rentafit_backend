package br.com.rentafit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request for changing an expired password.
 * Password: min 8 chars, ≥1 uppercase, ≥1 digit, ≥1 special char.
 */
public record ChangePasswordRequestDTO(

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#+]).{8,}$",
                message = "Password must contain at least 1 uppercase letter, 1 number, and 1 special character (@$!%*?&#+)"
        )
        String newPassword
) {
}
