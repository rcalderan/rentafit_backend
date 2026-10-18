package br.com.rentafit.finance.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resumo diário de movimentação financeira para alimentar o calendário mensal e gráfico diário.
 */
@Builder
public record DailyMovementSummaryDTO(
        LocalDate date,
        BigDecimal paidAmount,
        BigDecimal fineAmount,
        BigDecimal refundedAmount,
        BigDecimal netRealizedAmount,
        BigDecimal pendingAmount,
        BigDecimal rentalNetAmount,
        BigDecimal saleNetAmount,
        BigDecimal rentalPendingAmount,
        BigDecimal salePendingAmount,
        int transactionCount
) {
}
