package br.com.rentafit.finance.service;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.finance.dto.DailyMovementSummaryDTO;
import br.com.rentafit.finance.dto.FinancialTransactionDTO;
import br.com.rentafit.finance.dto.MonthlyBucketDTO;
import br.com.rentafit.finance.dto.MonthlyMovementResponseDTO;
import br.com.rentafit.finance.dto.MovementSourceFilter;
import br.com.rentafit.finance.dto.PaymentMethodBreakdownDTO;
import br.com.rentafit.finance.dto.YearlyMovementResponseDTO;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.repository.RentalPaymentRepository;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.SalesPayment;
import br.com.rentafit.sales.repository.SalesPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Serviço de consolidação da movimentação financeira de locações e vendas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FinancialMovementService {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private final RentalPaymentRepository rentalPaymentRepository;
    private final SalesPaymentRepository salesPaymentRepository;

    /**
     * Gera o consolidado mensal (resumo diário 1..31, meios de pagamento e extrato de parcelas).
     *
     * <p>Exemplo: {@code service.getMonthlyMovement(2026, 10, "ALL");}</p>
     */
    public MonthlyMovementResponseDTO getMonthlyMovement(int year, int month, String sourceParam) {
        validateYearAndMonth(year, month);
        MovementSourceFilter filter = MovementSourceFilter.fromParam(sourceParam);
        YearMonth yearMonth = YearMonth.of(year, month);
        List<FinancialTransactionDTO> transactions = loadTransactions(
                yearMonth.atDay(1), yearMonth.atEndOfMonth(), filter);

        MovementAccumulator totals = accumulateAll(transactions);
        List<DailyMovementSummaryDTO> days = buildDailySummaries(yearMonth, transactions);
        List<PaymentMethodBreakdownDTO> byMethod = buildMethodBreakdown(transactions);

        log.info("Loaded monthly financial movement for {}/{} (source={}, count={})",
                year, month, filter, transactions.size());
        return MonthlyMovementResponseDTO.builder()
                .year(year)
                .month(month)
                .paidTotal(totals.getPaidAmount())
                .fineTotal(totals.getFineAmount())
                .refundedTotal(totals.getRefundedAmount())
                .netRealizedTotal(totals.getNetRealizedAmount())
                .pendingTotal(totals.getPendingAmount())
                .rentalNetTotal(totals.getRentalNetAmount())
                .saleNetTotal(totals.getSaleNetAmount())
                .rentalPendingTotal(totals.getRentalPendingAmount())
                .salePendingTotal(totals.getSalePendingAmount())
                .byPaymentMethod(byMethod)
                .days(days)
                .transactions(transactions)
                .build();
    }

    /**
     * Gera o consolidado anual (12 meses de Jan a Dez e meios de pagamento).
     *
     * <p>Exemplo: {@code service.getYearlyMovement(2026, "ALL");}</p>
     */
    public YearlyMovementResponseDTO getYearlyMovement(int year, String sourceParam) {
        validateYear(year);
        MovementSourceFilter filter = MovementSourceFilter.fromParam(sourceParam);
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<FinancialTransactionDTO> transactions = loadTransactions(start, end, filter);

        MovementAccumulator totals = accumulateAll(transactions);
        List<MonthlyBucketDTO> months = buildMonthlyBuckets(transactions);
        List<PaymentMethodBreakdownDTO> byMethod = buildMethodBreakdown(transactions);
        List<Integer> availableYears = resolveAvailableYears(year);

        log.info("Loaded yearly financial movement for {} (source={}, count={})",
                year, filter, transactions.size());
        return YearlyMovementResponseDTO.builder()
                .year(year)
                .availableYears(availableYears)
                .paidTotal(totals.getPaidAmount())
                .fineTotal(totals.getFineAmount())
                .refundedTotal(totals.getRefundedAmount())
                .netRealizedTotal(totals.getNetRealizedAmount())
                .pendingTotal(totals.getPendingAmount())
                .rentalNetTotal(totals.getRentalNetAmount())
                .saleNetTotal(totals.getSaleNetAmount())
                .rentalPendingTotal(totals.getRentalPendingAmount())
                .salePendingTotal(totals.getSalePendingAmount())
                .byPaymentMethod(byMethod)
                .months(months)
                .build();
    }

    private void validateYearAndMonth(int year, int month) {
        validateYear(year);
        if (month < 1 || month > 12) {
            throw new ValidationException(
                    "Invalid month '" + month + "'. Expected integer between 1 and 12");
        }
    }

    private void validateYear(int year) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new ValidationException(
                    "Invalid year '" + year + "'. Expected integer between " + MIN_YEAR + " and " + MAX_YEAR);
        }
    }

    private List<FinancialTransactionDTO> loadTransactions(
            LocalDate startDate, LocalDate endDate, MovementSourceFilter filter) {
        List<FinancialTransactionDTO> items = new ArrayList<>();
        if (filter.includesRental()) {
            rentalPaymentRepository.findActiveMovementsBetween(startDate, endDate).stream()
                    .map(this::mapRentalPayment)
                    .forEach(items::add);
        }
        if (filter.includesSale()) {
            salesPaymentRepository.findActiveMovementsBetween(startDate, endDate).stream()
                    .map(this::mapSalesPayment)
                    .forEach(items::add);
        }
        items.sort(Comparator.comparing(FinancialTransactionDTO::paymentDate)
                .thenComparing(FinancialTransactionDTO::source)
                .thenComparing(FinancialTransactionDTO::referenceCode, Comparator.nullsLast(String::compareTo)));
        return items;
    }

    private FinancialTransactionDTO mapRentalPayment(RentalPayment payment) {
        RentalContract contract = payment.getContract();
        String code = contract.getLegacyId() != null && !contract.getLegacyId().isBlank()
                ? contract.getLegacyId()
                : contract.getId().toString().substring(0, 8);
        return FinancialTransactionDTO.builder()
                .paymentId(payment.getId())
                .source("RENTAL")
                .referenceId(contract.getId())
                .referenceCode(code)
                .customerName(contract.getCustomerName() != null ? contract.getCustomerName() : "Cliente")
                .installmentNumber(payment.getInstallmentNumber())
                .installments(payment.getInstallments() != null ? payment.getInstallments() : 1)
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod().name())
                .status(payment.getStatus().name())
                .value(payment.getValue())
                .signedValue(resolveSignedValue(payment.getStatus(), payment.getValue()))
                .build();
    }

    private FinancialTransactionDTO mapSalesPayment(SalesPayment payment) {
        SalesOrder order = payment.getSalesOrder();
        String code = order.getLegacyId() != null && !order.getLegacyId().isBlank()
                ? order.getLegacyId()
                : order.getId().toString().substring(0, 8);
        String customer = order.getCustomerName() != null && !order.getCustomerName().isBlank()
                ? order.getCustomerName()
                : "Venda Balcão";
        return FinancialTransactionDTO.builder()
                .paymentId(payment.getId())
                .source("SALE")
                .referenceId(order.getId())
                .referenceCode(code)
                .customerName(customer)
                .installmentNumber(payment.getInstallmentNumber())
                .installments(payment.getInstallments() != null ? payment.getInstallments() : 1)
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod().name())
                .status(payment.getStatus().name())
                .value(payment.getValue())
                .signedValue(resolveSignedValue(payment.getStatus(), payment.getValue()))
                .build();
    }

    private BigDecimal resolveSignedValue(PaymentStatus status, BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return status == PaymentStatus.REFUNDED ? value.negate() : value;
    }

    private MovementAccumulator accumulateAll(List<FinancialTransactionDTO> transactions) {
        MovementAccumulator acc = new MovementAccumulator();
        transactions.forEach(acc::accept);
        return acc;
    }

    private List<DailyMovementSummaryDTO> buildDailySummaries(
            YearMonth yearMonth, List<FinancialTransactionDTO> transactions) {
        Map<LocalDate, MovementAccumulator> byDay = new LinkedHashMap<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            byDay.put(yearMonth.atDay(day), new MovementAccumulator());
        }
        for (FinancialTransactionDTO tx : transactions) {
            MovementAccumulator dayAcc = byDay.get(tx.paymentDate());
            if (dayAcc != null) {
                dayAcc.accept(tx);
            }
        }
        return byDay.entrySet().stream()
                .map(entry -> entry.getValue().toDailySummary(entry.getKey()))
                .toList();
    }

    private List<MonthlyBucketDTO> buildMonthlyBuckets(List<FinancialTransactionDTO> transactions) {
        Map<Integer, MovementAccumulator> byMonth = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            byMonth.put(m, new MovementAccumulator());
        }
        for (FinancialTransactionDTO tx : transactions) {
            MovementAccumulator monthAcc = byMonth.get(tx.paymentDate().getMonthValue());
            if (monthAcc != null) {
                monthAcc.accept(tx);
            }
        }
        return byMonth.entrySet().stream()
                .map(entry -> entry.getValue().toMonthlyBucket(entry.getKey()))
                .toList();
    }

    private List<PaymentMethodBreakdownDTO> buildMethodBreakdown(List<FinancialTransactionDTO> transactions) {
        Map<PaymentMethod, MovementAccumulator> byMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            byMethod.put(method, new MovementAccumulator());
        }
        for (FinancialTransactionDTO tx : transactions) {
            PaymentMethod method = PaymentMethod.valueOf(tx.paymentMethod());
            byMethod.get(method).accept(tx);
        }
        return byMethod.entrySet().stream()
                .map(entry -> entry.getValue().toMethodBreakdown(entry.getKey().name()))
                .toList();
    }

    private List<Integer> resolveAvailableYears(int requestedYear) {
        TreeSet<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        years.add(LocalDate.now().getYear());
        years.add(requestedYear);
        years.addAll(rentalPaymentRepository.findDistinctPaymentYears());
        years.addAll(salesPaymentRepository.findDistinctPaymentYears());
        return years.stream().toList();
    }
}
