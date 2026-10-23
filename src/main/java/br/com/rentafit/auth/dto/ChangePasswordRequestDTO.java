package br.com.rentafit.auth.dto;

import br.com.rentafit.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Request for changing an expired password.
 * Password: see {@link br.com.rentafit.common.validation.PasswordRules}.
 */
public record ChangePasswordRequestDTO(

        @NotBlank(message = "Senha é obrigatória")
        @StrongPassword
        String newPassword
) {
    @Override
    public String toString() {
        return "ChangePasswordRequestDTO[credentials=REDACTED]";
    }
}
