package br.com.rentafit.billing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Requisição para sincronizar um documento fiscal emitido externamente
 * (ex.: NF-e emitida pelo microsserviço costume-rental-nfe) com a base do Rentafit.
 */
public record FiscalDocumentSyncRequest(
        String type,
        String origin,
        UUID originId,
        String accessKey,
        Long number,
        String series,
        String protocol,
        String status,
        BigDecimal totalValue,
        String customerName,
        String customerDocument,
        String customerEmail,
        OffsetDateTime issueDate,
        String authorizedXml,
        String rejectionReason,
        String cancelReason,
        OffsetDateTime cancelledAt,
        String cancelProtocol
) {
}
