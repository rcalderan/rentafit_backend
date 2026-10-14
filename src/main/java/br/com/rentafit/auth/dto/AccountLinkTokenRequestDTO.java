package br.com.rentafit.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Token recebido no link enviado por e-mail")
public record AccountLinkTokenRequestDTO(
        @NotBlank(message = "Token é obrigatório")
        @Pattern(regexp = "^[A-Za-z0-9_-]{20,128}$", message = "Token em formato inválido (esperado: base64url, 20 a 128 caracteres)")
        String token
) {
    @Override
    public String toString() {
        return "AccountLinkTokenRequestDTO[token=REDACTED]";
    }
}
