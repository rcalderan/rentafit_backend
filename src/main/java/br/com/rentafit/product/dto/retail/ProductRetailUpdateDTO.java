package br.com.rentafit.product.dto.retail;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRetailUpdateDTO(
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        String sku,

        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Size(max = 50)
        String size,

        @Size(max = 50)
        String color,

        @Size(max = 100)
        String brand,
        String details,

        @DecimalMin("0.01")
        @DecimalMax("9999999999.99")
        BigDecimal value,

        String description
) {
}
