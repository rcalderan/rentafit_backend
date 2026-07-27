package br.com.rentafit.billing.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Projeção enxuta de {@code FiscalDocument} para listagens.
 * Não expõe XML, dados completos do cliente nem informações sensíveis.
 */
@Builder
public record FiscalDocumentSummaryResponse(
        UUID id,
        String type,
        String status,
        Long number,
        String series,
        String accessKey,
        OffsetDateTime emissionDate,
        BigDecimal value,
        String customerName,
        String origin,
        UUID originId
) {
}
