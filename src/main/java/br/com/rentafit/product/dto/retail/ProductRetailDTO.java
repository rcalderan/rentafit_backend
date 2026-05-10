package br.com.rentafit.product.dto.retail;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ProductRetailDTO(
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    String sku,

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotNull(message = "Category is required")
    UUID categoryId,

    @NotBlank(message = "Size is required")
    @Size(max = 50)
    String size,

    @Size(max = 50)
    String color,

    @Size(max = 100)
    String brand,
    String details,

    @NotNull(message = "Value is required")
    @DecimalMin("0.01")
    @DecimalMax("9999999999.99")
    BigDecimal value,

    String description,

    Integer warrantyDays
) {}
