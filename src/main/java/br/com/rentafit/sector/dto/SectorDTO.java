package br.com.rentafit.sector.dto;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de saída de um setor de atendimento/retirada vinculado a uma Matriz/Filial.
 */
@Builder
public record SectorDTO(
        UUID id,
        String issuerCnpj,
        String branchName,
        String name,
        String code,
        Boolean active,
        Boolean isDefault,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
