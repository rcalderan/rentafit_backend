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
        @Digits(integer = 10, fraction = 2, message = "Value must have at most 2 decimal places")
        BigDecimal value,

        String description,

        @Min(value = 0, message = "Warranty days must not be negative")
        @Max(value = 3650, message = "Warranty days must not exceed 3650")
        Integer warrantyDays
) {
}
