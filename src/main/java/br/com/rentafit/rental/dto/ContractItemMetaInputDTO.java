package br.com.rentafit.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Metadado de um item de contrato (acessório ou observação).
 */
public record ContractItemMetaInputDTO(

        @NotBlank(message = "Tipo do metadado é obrigatório (ACESSORIO ou OBSERVACAO)")
        String type,

        @NotBlank(message = "Descrição do metadado é obrigatória")
        String description,

        /** UUID do acessório catalogado. Null para texto livre. */
        UUID accessoryId
) {}

