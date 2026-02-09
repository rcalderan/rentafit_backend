package br.com.rentafit.product.dto.retail;

import br.com.rentafit.product.dto.StockDTO;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ProductRetailDetailsDTO(
    UUID id,
    String name,
    String categoryName,
    String size,
    String color,
    String brand,
    BigDecimal value,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String details,
    String sku,
    StockDTO stock
) {}
