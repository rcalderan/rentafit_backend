package br.com.rentafit.billing.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representação completa de {@code FiscalDocument} para a tela de detalhes.
 * Não expõe XML assinado/autorizado nem entidades JPA.
 */
@Builder
public record FiscalDocumentDetailResponse(
        UUID id,
        String type,
        String status,
        Long number,
        String series,
        String accessKey,
        OffsetDateTime emissionDate,
        String protocol,
        BigDecimal value,
        String serviceDescription,
        String cancelReason,
        OffsetDateTime cancelledAt,
        String cancelProtocol,
        String customerEmail,
        String customerName,
        String origin,
        UUID originId
) {
}
