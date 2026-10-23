package br.com.rentafit.sector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para criação de um setor vinculado a uma Matriz/Filial.
 */
public record CreateSectorRequestDTO(

        @NotBlank(message = "CNPJ do estabelecimento (Matriz/Filial) é obrigatório")
        @Pattern(regexp = "\\d{14}", message = "issuerCnpj deve conter exatamente 14 dígitos numéricos")
        String issuerCnpj,

        @Size(max = 255)
        String branchName,

        @NotBlank(message = "Nome do setor é obrigatório")
        @Size(max = 100)
        String name,

        @NotBlank(message = "Sigla do setor é obrigatória")
        @Size(max = 20)
        @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "Sigla deve conter apenas letras, números, '_' ou '-'")
        String code,

        Boolean isDefault
) {}
