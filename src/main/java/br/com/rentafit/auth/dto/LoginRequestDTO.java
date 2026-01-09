package br.com.rentafit.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request de login do usuário")
public record LoginRequestDTO(
        @NotBlank @Schema(example = "admin") String username,
        @NotBlank @Schema(example = "admin123") String password
) {}

