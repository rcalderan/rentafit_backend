package br.com.rentafit.product.dto.rental;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record RentalItemDetailsDTO(
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
        Integer legacyId,
        String status,
        String notes,
        String condition,
        LocalDateTime lastRentalDate,
        Integer rentalCount,
        LocalDate maintenanceDueDate
) {}
