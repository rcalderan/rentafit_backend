package br.com.rentafit.finance.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Totais consolidados por forma de pagamento (CASH, PIX, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER).
 */
@Builder
public record PaymentMethodBreakdownDTO(
        String paymentMethod,
        BigDecimal paidAmount,
        BigDecimal fineAmount,
        BigDecimal refundedAmount,
        BigDecimal netRealizedAmount,
        BigDecimal pendingAmount,
        int transactionCount
) {
}
