package br.com.rentafit.sales.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de saída detalhado de um item do pedido de venda.
 */
@Builder
public record SalesOrderItemDetailsDTO(
        UUID id,
        UUID retailProductId,
        String sku,
        String description,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal discountValue,
        BigDecimal totalValue,
        String itemStatus,
        String itemStatusDescription,
        UUID attendantEmployeeId,
        Boolean needsTailoring,
        String tailoringNotes,
        OffsetDateTime deliveredAt,
        UUID deliveredByEmployeeId,
        Integer warrantyDays
) {}
