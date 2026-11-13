package br.com.rentafit.product.dto.rental;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record RentalItemUpdateDTO(

        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        UUID categoryId,

        @Size(max = 50)
        String size,

        @Size(max = 50)
        String color,

        @Size(max = 100)
        String brand,

        @DecimalMin("0.01")
        @DecimalMax("9999999999.99")
        @Digits(integer = 10, fraction = 2, message = "Value must have at most 2 decimal places")
        BigDecimal value,
        String description,

        @Size(max = 20)
        @Pattern(regexp = "^(AVAILABLE|RENTED|RESERVED|MAINTENANCE|RETIRED)$", message = "Invalid status")
        String status,
        String notes,

        @Size(max = 20)
        @Pattern(regexp = "^(NEW|EXCELLENT|GOOD|FAIR|POOR)$", message = "Invalid condition")
        String condition) {
}
