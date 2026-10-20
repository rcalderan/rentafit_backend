package br.com.rentafit.finance.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta consolidada da movimentação financeira de um mês específico.
 */
@Builder
public record MonthlyMovementResponseDTO(
        int year,
        int month,
        BigDecimal paidTotal,
        BigDecimal fineTotal,
        BigDecimal refundedTotal,
        BigDecimal netRealizedTotal,
        BigDecimal pendingTotal,
        BigDecimal rentalNetTotal,
        BigDecimal saleNetTotal,
        BigDecimal rentalPendingTotal,
        BigDecimal salePendingTotal,
        List<PaymentMethodBreakdownDTO> byPaymentMethod,
        List<SectorMovementBreakdownDTO> bySector,
        List<DailyMovementSummaryDTO> days,
        List<FinancialTransactionDTO> transactions
) {
}
