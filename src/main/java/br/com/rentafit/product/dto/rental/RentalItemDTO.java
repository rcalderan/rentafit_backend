package br.com.rentafit.product.dto.rental;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record RentalItemDTO(
    UUID id,
    @Positive(message = "Legacy ID must be a positive number")
    Integer legacyId,

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotNull(message = "Category is required")
    UUID categoryId,

    @NotBlank(message = "Size is required")
    @Size(max = 50)
    String size,

    @NotBlank(message = "Color is required")
    @Size(max = 50)
    String color,

    @Size(max = 100)
    String brand,

    @NotNull(message = "Value is required")
    @DecimalMin("0.01")
    @DecimalMax("9999999999.99")
    @Digits(integer = 10, fraction = 2, message = "Value must have at most 2 decimal places")
    BigDecimal value,
    String description,

    @Size(max = 20)
    @Pattern(regexp = "^(AVAILABLE|RENTED|RESERVED|MAINTENANCE|RETIRED)$", message = "Invalid status")
    String status,
    String notes,

    @NotBlank(message = "Condition is required")
    @Size(max = 20)
    @Pattern(regexp = "^(NEW|EXCELLENT|GOOD|FAIR|POOR)$", message = "Invalid condition")
    String condition
) {}
