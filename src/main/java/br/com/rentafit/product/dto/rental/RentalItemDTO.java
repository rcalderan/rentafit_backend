package br.com.rentafit.product.dto.rental;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record RentalItemDTO(
    UUID id,
    @Size(max = 50)
    String legacyId,

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotNull(message = "Category is required")
    UUID categoryId,

    @Size(max = 50)
    String size,

    @Size(max = 50)
    String color,

    @Size(max = 100)
    String brand,

    @NotNull(message = "Value is required")
    @DecimalMin("0.01")
    @DecimalMax("9999999999.99")
    BigDecimal value,
    String description,

    @Size(max = 20)
    String status,
    String notes,

    @Size(max = 20)
    String condition
) {}
