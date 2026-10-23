package br.com.rentafit.sector.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para atualização de um setor existente.
 * Campos nulos são preservados (edição parcial).
 */
public record UpdateSectorRequestDTO(

        @Size(max = 255)
        String branchName,

        @Size(max = 100)
        String name,

        @Size(max = 20)
        @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "Sigla deve conter apenas letras, números, '_' ou '-'")
        String code,

        Boolean active
) {}
