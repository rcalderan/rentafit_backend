package br.com.rentafit.finance.service;

import br.com.rentafit.finance.dto.DailyMovementSummaryDTO;
import br.com.rentafit.finance.dto.FinancialTransactionDTO;
import br.com.rentafit.finance.dto.MonthlyBucketDTO;
import br.com.rentafit.finance.dto.PaymentMethodBreakdownDTO;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Acumulador aritmético para consolidar parcelas (PAID, MULTA, REFUNDED e PENDING)
 * sem duplicar regras de soma e subtração de estornos.
 */
@Getter
public class MovementAccumulator {

    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal fineAmount = BigDecimal.ZERO;
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    private BigDecimal pendingAmount = BigDecimal.ZERO;
    private BigDecimal rentalNetAmount = BigDecimal.ZERO;
    private BigDecimal saleNetAmount = BigDecimal.ZERO;
    private BigDecimal rentalPendingAmount = BigDecimal.ZERO;
    private BigDecimal salePendingAmount = BigDecimal.ZERO;
    private int transactionCount = 0;

    /**
     * Incorpora uma transação financeira ao acumulador respeitando o status e a origem.
     *
     * <p>Exemplo: {@code accumulator.accept(transactionDto);}</p>
     */
    public void accept(FinancialTransactionDTO tx) {
        if (tx == null || tx.value() == null) {
            return;
        }
        transactionCount++;
        switch (tx.status()) {
            case "PAID" -> recordPaid(tx.source(), tx.value());
            case "MULTA" -> recordFine(tx.source(), tx.value());
            case "REFUNDED" -> recordRefund(tx.source(), tx.value());
            case "PENDING" -> recordPending(tx.source(), tx.value());
            default -> { }
        }
    }

    private void recordPaid(String source, BigDecimal value) {
        paidAmount = paidAmount.add(value);
        addRealizedBySource(source, value);
    }

    private void recordFine(String source, BigDecimal value) {
        fineAmount = fineAmount.add(value);
        addRealizedBySource(source, value);
    }

    private void recordRefund(String source, BigDecimal value) {
        refundedAmount = refundedAmount.add(value);
        addRealizedBySource(source, value.negate());
    }

    private void recordPending(String source, BigDecimal value) {
        pendingAmount = pendingAmount.add(value);
        if ("RENTAL".equals(source)) {
            rentalPendingAmount = rentalPendingAmount.add(value);
            return;
        }
        salePendingAmount = salePendingAmount.add(value);
    }

    private void addRealizedBySource(String source, BigDecimal delta) {
        if ("RENTAL".equals(source)) {
            rentalNetAmount = rentalNetAmount.add(delta);
            return;
        }
        saleNetAmount = saleNetAmount.add(delta);
    }

    /**
     * Calcula o valor líquido efetivamente realizado: {@code PAID + MULTA - REFUNDED}.
     *
     * <p>Exemplo: {@code BigDecimal net = accumulator.getNetRealizedAmount();}</p>
     */
    public BigDecimal getNetRealizedAmount() {
        return paidAmount.add(fineAmount).subtract(refundedAmount);
    }

    /**
     * Converte o estado acumulado em {@link PaymentMethodBreakdownDTO}.
     *
     * <p>Exemplo: {@code PaymentMethodBreakdownDTO dto = accumulator.toMethodBreakdown("PIX");}</p>
     */
    public PaymentMethodBreakdownDTO toMethodBreakdown(String paymentMethod) {
        return PaymentMethodBreakdownDTO.builder()
                .paymentMethod(paymentMethod)
                .paidAmount(paidAmount)
                .fineAmount(fineAmount)
                .refundedAmount(refundedAmount)
                .netRealizedAmount(getNetRealizedAmount())
                .pendingAmount(pendingAmount)
                .transactionCount(transactionCount)
                .build();
    }

    /**
     * Converte o estado acumulado em {@link DailyMovementSummaryDTO}.
     *
     * <p>Exemplo: {@code DailyMovementSummaryDTO dto = accumulator.toDailySummary(date);}</p>
     */
    public DailyMovementSummaryDTO toDailySummary(LocalDate date) {
        return DailyMovementSummaryDTO.builder()
                .date(date)
                .paidAmount(paidAmount)
                .fineAmount(fineAmount)
                .refundedAmount(refundedAmount)
                .netRealizedAmount(getNetRealizedAmount())
                .pendingAmount(pendingAmount)
                .rentalNetAmount(rentalNetAmount)
                .saleNetAmount(saleNetAmount)
                .rentalPendingAmount(rentalPendingAmount)
                .salePendingAmount(salePendingAmount)
                .transactionCount(transactionCount)
                .build();
    }

    /**
     * Converte o estado acumulado em {@link MonthlyBucketDTO}.
     *
     * <p>Exemplo: {@code MonthlyBucketDTO dto = accumulator.toMonthlyBucket(10);}</p>
     */
    public MonthlyBucketDTO toMonthlyBucket(int month) {
        return MonthlyBucketDTO.builder()
                .month(month)
                .paidAmount(paidAmount)
                .fineAmount(fineAmount)
                .refundedAmount(refundedAmount)
                .netRealizedAmount(getNetRealizedAmount())
                .pendingAmount(pendingAmount)
                .rentalNetAmount(rentalNetAmount)
                .saleNetAmount(saleNetAmount)
                .rentalPendingAmount(rentalPendingAmount)
                .salePendingAmount(salePendingAmount)
                .transactionCount(transactionCount)
                .build();
    }
}
