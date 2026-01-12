package br.com.rentafit.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitação de novo token de acesso")
public record TokenRefreshRequestDTO(
        @NotBlank @Schema(description = "Token de renovação persistido") String refreshToken
) {}

