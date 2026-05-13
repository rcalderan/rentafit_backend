package br.com.rentafit.rental.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO resumido para listagem paginada. Sem itens e pagamentos para melhor performance.
 */
@Builder
public record RentalContractSummaryDTO(
        UUID id,
        String legacyId,
        Integer contractType,
        String status,
        String statusDescription,
        UUID customerId,
        String customerName,
        UUID parentContractId,
        UUID replacedByContractId,
        LocalDate eventDate,
        LocalDate pickupDate,
        LocalDate returnDate,
        Boolean returned,
        BigDecimal totalValue,
        BigDecimal paidValue,
        OffsetDateTime createdAt
) {}
