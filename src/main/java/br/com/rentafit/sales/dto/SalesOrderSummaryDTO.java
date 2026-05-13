package br.com.rentafit.sales.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO resumido para listagem paginada. Sem itens e pagamentos para melhor performance.
 */
@Builder
public record SalesOrderSummaryDTO(
        UUID id,
        String legacyId,
        String status,
        String statusDescription,
        UUID customerId,
        String customerName,
        String invoiceStatus,
        BigDecimal totalValue,
        BigDecimal paidValue,
        Integer itemCount,
        OffsetDateTime createdAt
) {}
