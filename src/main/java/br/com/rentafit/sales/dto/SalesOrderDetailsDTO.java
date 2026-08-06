package br.com.rentafit.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de saída detalhado de um pedido de venda.
 */
@Builder
public record SalesOrderDetailsDTO(
        UUID id,
        String legacyId,
        String status,
        String statusDescription,

        // Snapshot do cliente (null para venda balcão)
        UUID customerId,
        String customerName,
        String customerDocument,

        UUID createdByEmployeeId,

        String notes,
        BigDecimal discountValue,

        // Documento fiscal vinculado (NF-e / NFS-e)
        String invoiceStatus,
        String invoiceStatusDescription,
        String invoiceId,
        String invoiceNumber,
        String invoiceSeries,
        String invoiceAccessKey,
        OffsetDateTime invoiceEmissionDate,
        String invoiceProtocol,
        String invoiceCancelReason,
        OffsetDateTime invoiceCancelledAt,
        String invoiceCancelProtocol,
        String invoiceXmlUrl,
        String invoiceCustomerEmail,
        String invoiceNatureOperation,

        // Valores calculados
        BigDecimal subtotal,
        BigDecimal totalValue,
        BigDecimal paidValue,
        BigDecimal remainingValue,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,

        List<SalesOrderItemDetailsDTO> items,
        List<SalesPaymentDetailsDTO> payments,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<String> warnings
) {}
