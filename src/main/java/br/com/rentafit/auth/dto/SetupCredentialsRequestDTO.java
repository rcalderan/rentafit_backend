package br.com.rentafit.auth.dto;

import br.com.rentafit.common.validation.StrongPassword;
import br.com.rentafit.common.validation.ValidPin;
import jakarta.validation.constraints.NotBlank;

/**
 * Request for first-access credential setup (password + PIN).
 * Password: see {@link br.com.rentafit.common.validation.PasswordRules}.
 * PIN: see {@link br.com.rentafit.common.validation.PinRules}.
 */
public record SetupCredentialsRequestDTO(

        @NotBlank(message = "Senha é obrigatória")
        @StrongPassword
        String newPassword,

        @NotBlank(message = "PIN é obrigatório")
        @ValidPin
        String pin
) {
    @Override
    public String toString() {
        return "SetupCredentialsRequestDTO[credentials=REDACTED]";
    }
}
