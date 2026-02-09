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
        BigDecimal value,
        String description,

        @Size(max = 20)
        String status,
        String notes,

        @Size(max = 20)
        String condition) {
}
