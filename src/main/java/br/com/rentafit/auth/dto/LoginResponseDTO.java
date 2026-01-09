package br.com.rentafit.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de autenticação bem-sucedida")
public record LoginResponseDTO(
        @Schema(description = "Token de acesso (JWT)") String accessToken,
        @Schema(description = "Token de renovação") String refreshToken,
        @Schema(description = "Tipo do token", example = "Bearer") String tokenType
) {}

