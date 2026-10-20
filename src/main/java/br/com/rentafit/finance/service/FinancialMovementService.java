package br.com.rentafit.finance.service;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.finance.dto.DailyMovementSummaryDTO;
import br.com.rentafit.finance.dto.FinancialTransactionDTO;
import br.com.rentafit.finance.dto.MonthlyBucketDTO;
import br.com.rentafit.finance.dto.MonthlyMovementResponseDTO;
import br.com.rentafit.finance.dto.MovementSourceFilter;
import br.com.rentafit.finance.dto.PaymentMethodBreakdownDTO;
import br.com.rentafit.finance.dto.SectorMovementBreakdownDTO;
import br.com.rentafit.finance.dto.YearlyMovementResponseDTO;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.repository.RentalPaymentRepository;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.SalesPayment;
import br.com.rentafit.sales.repository.SalesPaymentRepository;
import br.com.rentafit.sector.domain.Sector;
import br.com.rentafit.sector.repository.SectorRepository;
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
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final SectorRepository sectorRepository;

    /**
     * Gera o consolidado mensal (resumo diário 1..31, meios de pagamento, setores e extrato de parcelas).
     *
     * <p>{@code sectorId} restringe às movimentações daquele setor (caixa recebedor,
     * com fallback ao setor de origem do contrato/pedido para registros legados).
     * {@code issuerCnpj} restringe às movimentações de setores do estabelecimento.</p>
     *
     * <p>Exemplo: {@code service.getMonthlyMovement(2026, 10, "ALL", null, null);}</p>
     */
    public MonthlyMovementResponseDTO getMonthlyMovement(int year, int month, String sourceParam,
                                                       UUID sectorId, String issuerCnpj) {
        validateYearAndMonth(year, month);
        MovementSourceFilter filter = MovementSourceFilter.fromParam(sourceParam);
        YearMonth yearMonth = YearMonth.of(year, month);
        List<FinancialTransactionDTO> transactions = loadTransactions(
                yearMonth.atDay(1), yearMonth.atEndOfMonth(), filter, sectorId, issuerCnpj);

        MovementAccumulator totals = accumulateAll(transactions);
        List<DailyMovementSummaryDTO> days = buildDailySummaries(yearMonth, transactions);
        List<PaymentMethodBreakdownDTO> byMethod = buildMethodBreakdown(transactions);
        List<SectorMovementBreakdownDTO> bySector = buildSectorBreakdown(transactions);

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
                .bySector(bySector)
                .days(days)
                .transactions(transactions)
                .build();
    }

    /**
     * Gera o consolidado anual (12 meses de Jan a Dez, meios de pagamento e setores).
     *
     * <p>Exemplo: {@code service.getYearlyMovement(2026, "ALL", null, null);}</p>
     */
    public YearlyMovementResponseDTO getYearlyMovement(int year, String sourceParam,
                                                      UUID sectorId, String issuerCnpj) {
        validateYear(year);
        MovementSourceFilter filter = MovementSourceFilter.fromParam(sourceParam);
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<FinancialTransactionDTO> transactions = loadTransactions(start, end, filter, sectorId, issuerCnpj);

        MovementAccumulator totals = accumulateAll(transactions);
        List<MonthlyBucketDTO> months = buildMonthlyBuckets(transactions);
        List<PaymentMethodBreakdownDTO> byMethod = buildMethodBreakdown(transactions);
        List<SectorMovementBreakdownDTO> bySector = buildSectorBreakdown(transactions);
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
                .bySector(bySector)
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
            LocalDate startDate, LocalDate endDate, MovementSourceFilter filter,
            UUID sectorId, String issuerCnpj) {
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

        Map<UUID, Sector> sectorsById = resolveSectorMap(items);
        items = items.stream()
                .map(tx -> enrichWithSector(tx, sectorsById))
                .filter(tx -> matchesSectorFilter(tx, sectorsById, sectorId, issuerCnpj))
                .collect(Collectors.toCollection(ArrayList::new));

        items.sort(Comparator.comparing(FinancialTransactionDTO::paymentDate)
                .thenComparing(FinancialTransactionDTO::source)
                .thenComparing(FinancialTransactionDTO::referenceCode, Comparator.nullsLast(String::compareTo)));
        return items;
    }

    /** Carrega em lote os setores referenciados pelas transações. */
    private Map<UUID, Sector> resolveSectorMap(List<FinancialTransactionDTO> items) {
        List<UUID> ids = items.stream()
                .map(FinancialTransactionDTO::sectorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return sectorRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Sector::getId, s -> s));
    }

    private FinancialTransactionDTO enrichWithSector(FinancialTransactionDTO tx, Map<UUID, Sector> sectorsById) {
        Sector sector = tx.sectorId() == null ? null : sectorsById.get(tx.sectorId());
        if (sector == null) {
            return tx;
        }
        return FinancialTransactionDTO.builder()
                .paymentId(tx.paymentId())
                .source(tx.source())
                .referenceId(tx.referenceId())
                .referenceCode(tx.referenceCode())
                .customerName(tx.customerName())
                .installmentNumber(tx.installmentNumber())
                .installments(tx.installments())
                .paymentDate(tx.paymentDate())
                .paymentMethod(tx.paymentMethod())
                .status(tx.status())
                .value(tx.value())
                .signedValue(tx.signedValue())
                .sectorId(sector.getId())
                .sectorName(sector.getName())
                .sectorCode(sector.getCode())
                .issuerCnpj(sector.getIssuerCnpj())
                .build();
    }

    private boolean matchesSectorFilter(FinancialTransactionDTO tx, Map<UUID, Sector> sectorsById,
                                        UUID sectorId, String issuerCnpj) {
        if (sectorId != null && !sectorId.equals(tx.sectorId())) {
            return false;
        }
        if (issuerCnpj == null || issuerCnpj.isBlank()) {
            return true;
        }
        Sector sector = tx.sectorId() == null ? null : sectorsById.get(tx.sectorId());
        return sector != null && issuerCnpj.equals(sector.getIssuerCnpj());
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
                .sectorId(resolvePaymentSector(payment.getSectorId(), contract.getSectorId()))
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
                .sectorId(resolvePaymentSector(payment.getSectorId(), order.getSectorId()))
                .build();
    }

    /**
     * Setor contábil da parcela: para movimentações de caixa (PAID/MULTA/REFUNDED)
     * é o setor do terminal recebedor; para PENDING (ou registros legados sem
     * setor na parcela) cai no setor de origem do contrato/pedido.
     */
    private UUID resolvePaymentSector(UUID paymentSectorId, UUID originSectorId) {
        return paymentSectorId != null ? paymentSectorId : originSectorId;
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

    /**
     * Consolida as movimentações por setor de caixa para o fechamento diário/mensal.
     * Transações sem setor (legado pré-V44) caem no grupo {@code null} — "Sem setor".
     */
    private List<SectorMovementBreakdownDTO> buildSectorBreakdown(List<FinancialTransactionDTO> transactions) {
        Map<UUID, MovementAccumulator> bySector = new LinkedHashMap<>();
        Map<UUID, FinancialTransactionDTO> labelSource = new LinkedHashMap<>();
        for (FinancialTransactionDTO tx : transactions) {
            bySector.computeIfAbsent(tx.sectorId(), k -> new MovementAccumulator()).accept(tx);
            labelSource.putIfAbsent(tx.sectorId(), tx);
        }
        return bySector.entrySet().stream()
                .map(e -> {
                    FinancialTransactionDTO label = labelSource.get(e.getKey());
                    return e.getValue().toSectorBreakdown(
                            e.getKey(),
                            e.getKey() == null ? "Sem setor (Legado)" : label.sectorName(),
                            label.sectorCode(),
                            label.issuerCnpj());
                })
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
