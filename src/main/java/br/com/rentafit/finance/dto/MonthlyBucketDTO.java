package br.com.rentafit.finance.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Resumo mensal (1..12) para a visão anual do painel financeiro.
 */
@Builder
public record MonthlyBucketDTO(
        int month,
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
