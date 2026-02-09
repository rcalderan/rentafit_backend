package br.com.rentafit.product.dto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record AccessoryDetailsDTO(
        UUID id,
        String legacyId,
        String name,
        String categoryName,
        String size,
        String color,
        String brand,
        BigDecimal value,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        StockDTO stock,

        // Accessory specific
        String compatibleWith
) {
}
