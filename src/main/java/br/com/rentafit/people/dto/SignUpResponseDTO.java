package br.com.rentafit.people.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do cadastro: a conta aguarda confirmação do e-mail")
public record SignUpResponseDTO(
        @Schema(description = "E-mail mascarado para onde o link de confirmação foi enviado", example = "j***@example.com")
        String maskedEmail
) {
}
