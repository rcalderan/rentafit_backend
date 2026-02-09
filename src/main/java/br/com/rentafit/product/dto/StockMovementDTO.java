package br.com.rentafit.product.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record StockMovementDTO(
    UUID stockId,
    String type,
    Integer quantity,
    LocalDateTime movementDate,
    UUID userId,
    String notes
) {}
