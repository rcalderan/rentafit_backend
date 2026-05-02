package br.com.rentafit.rental.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Resumo do estado de devolução de um contrato.
 *
 * <p>Espelha o modelo {@code ReturnSummaryModel} do frontend Angular para garantir
 * compatibilidade direta na Fase 3 (troca mock → HTTP).</p>
 */
@Builder
public record ReturnSummaryDTO(
        UUID contractId,
        String legacyId,
        String customerName,
        LocalDate returnDate,
        LocalDate actualReturnDate,
        int pendingCount,
        boolean isFullyReturned,
        long delayDays,
        BigDecimal suggestedFine,
        List<ReturnItemDTO> items,
        List<ReturnPaymentPreviewDTO> paymentsPreview
) {

    @Builder
    public record ReturnItemDTO(
            UUID itemId,
            String description,
            boolean isReturned,
            String returnedAt,
            String returnedBy,
            List<ReturnAccessoryDTO> accessories
    ) {}

    @Builder
    public record ReturnAccessoryDTO(
            UUID accessoryId,
            String description,
            boolean isReturned,
            String returnedAt
    ) {}

    @Builder
    public record ReturnPaymentPreviewDTO(
            int installmentNumber,
            BigDecimal value,
            String status
    ) {}
}
