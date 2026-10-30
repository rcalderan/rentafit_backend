package br.com.rentafit.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Preferências de interface do usuário autenticado")
public record UpdateUserPreferencesRequestDTO(
        @Schema(description = "Variante de interface ('simplified', 'legacy' ou 'atelier')", example = "atelier")
        String uiVariant,
        @Schema(description = "Visibilidade da barra lateral no desktop", example = "true")
        Boolean sidebarVisible
) {
}
