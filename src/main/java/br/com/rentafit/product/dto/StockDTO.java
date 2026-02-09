package br.com.rentafit.product.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record StockDTO(
    UUID productId,
    Integer quantityAvailable,
    Integer quantityReserved,
    Integer quantityTotal,
    Integer minStockLevel,
    String location,
    LocalDateTime lastMovementDate
) {}
