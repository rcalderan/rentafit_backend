package br.com.rentafit.rental.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entrada individual de devolução — item ou acessório de um contrato.
 *
 * <p>Quando {@code accessoryId} é nulo, a entrada refere-se ao item principal.
 * Quando preenchido, refere-se a um metadado do tipo ACESSORIO.</p>
 */
public record ReturnEntryDTO(

        @NotNull(message = "itemId é obrigatório")
        UUID itemId,

        UUID accessoryId,

        @NotNull(message = "returnedAt é obrigatório")
        OffsetDateTime returnedAt
) {}
