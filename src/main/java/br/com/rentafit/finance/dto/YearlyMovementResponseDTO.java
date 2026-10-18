package br.com.rentafit.finance.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta consolidada da movimentação financeira de um ano completo (Jan..Dez).
 */
@Builder
public record YearlyMovementResponseDTO(
        int year,
        List<Integer> availableYears,
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
        List<MonthlyBucketDTO> months
) {
}
