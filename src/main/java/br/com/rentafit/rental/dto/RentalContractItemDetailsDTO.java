package br.com.rentafit.rental.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RentalContractItemDetailsDTO(
        UUID id,
        UUID rentalItemId,
        String legacyProductCode,
        String description,
        BigDecimal value,
        Boolean delivered,
        UUID attendantEmployeeId,
        List<RentalContractItemMetaDTO> metadata
) {
    public record RentalContractItemMetaDTO(
            UUID id,
            String type,
            String typeDescription,
            String description,
            UUID accessoryId
    ) {}
}

