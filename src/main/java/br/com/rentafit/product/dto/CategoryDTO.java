package br.com.rentafit.product.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CategoryDTO(
    UUID id,
    String name,
    String displayName,
    String description,
    String productType,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
