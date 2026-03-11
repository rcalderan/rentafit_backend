package br.com.rentafit.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Item de locação no input do contrato.
 * O campo value é editável pelo usuário; description e legacyProductCode são snapshot do produto.
 */
public record ContractItemInputDTO(

        /** UUID do RentalItem no catálogo. Null aceito para itens legados sem correspondência. */
        UUID rentalItemId,

        /** Código legado do produto (busca via legacyId). */
        String legacyProductCode,

        @NotBlank(message = "Descrição do item é obrigatória")
        String description,

        @NotNull(message = "Valor do item é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor do item deve ser maior que zero")
        BigDecimal value,

        @Valid
        List<ContractItemMetaInputDTO> metadata
) {}

