package br.com.rentafit.finance.service;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.finance.dto.DailyMovementSummaryDTO;
import br.com.rentafit.finance.dto.MonthlyBucketDTO;
import br.com.rentafit.finance.dto.MonthlyMovementResponseDTO;
import br.com.rentafit.finance.dto.PaymentMethodBreakdownDTO;
import br.com.rentafit.finance.dto.SectorMovementBreakdownDTO;
import br.com.rentafit.finance.dto.YearlyMovementResponseDTO;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.repository.RentalPaymentRepository;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.SalesPayment;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.repository.SalesPaymentRepository;
import br.com.rentafit.sector.domain.Sector;
import br.com.rentafit.sector.repository.SectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinancialMovementService - Unit Tests")
class FinancialMovementServiceTest {

    @Mock
    private RentalPaymentRepository rentalPaymentRepository;

    @Mock
    private SalesPaymentRepository salesPaymentRepository;

    @Mock
    private SectorRepository sectorRepository;

    @InjectMocks
    private FinancialMovementService service;

    private RentalContract contract;
    private SalesOrder salesOrder;

    @BeforeEach
    void setUp() {
        contract = RentalContract.builder()
                .id(UUID.randomUUID())
                .legacyId("1042")
                .customerName("Maria Silva")
                .status(ContractStatus.FINALIZED)
                .build();

        salesOrder = SalesOrder.builder()
                .id(UUID.randomUUID())
                .legacyId("V-261017-1")
                .customerName("João Pereira")
                .status(SalesOrderStatus.PAID)
                .build();
    }

    @Test
    @DisplayName("Deve consolidar PAID, MULTA, REFUNDED e PENDING no movimento mensal de locações e vendas")
    void shouldConsolidateMonthlyMovementWithFinesAndRefunds() {
        LocalDate day10 = LocalDate.of(2026, 10, 10);
        LocalDate day17 = LocalDate.of(2026, 10, 17);

        RentalPayment paidRental = buildRentalPayment(day10, PaymentMethod.PIX, PaymentStatus.PAID, "500.00", 1);
        RentalPayment fineRental = buildRentalPayment(day17, PaymentMethod.CASH, PaymentStatus.MULTA, "50.00", 2);
        RentalPayment refundRental = buildRentalPayment(day17, PaymentMethod.CASH, PaymentStatus.REFUNDED, "120.00", 3);
        RentalPayment pendingRental = buildRentalPayment(day17, PaymentMethod.CREDIT_CARD, PaymentStatus.PENDING, "300.00", 4);

        SalesPayment paidSale = buildSalesPayment(day10, PaymentMethod.CREDIT_CARD, PaymentStatus.PAID, "200.00", 1);
        SalesPayment pendingSale = buildSalesPayment(day17, PaymentMethod.DEBIT_CARD, PaymentStatus.PENDING, "80.00", 2);

        when(rentalPaymentRepository.findActiveMovementsBetween(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(paidRental, fineRental, refundRental, pendingRental));
        when(salesPaymentRepository.findActiveMovementsBetween(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(paidSale, pendingSale));

        MonthlyMovementResponseDTO result = service.getMonthlyMovement(2026, 10, "ALL", null, null);

        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.month()).isEqualTo(10);
        assertThat(result.paidTotal()).isEqualByComparingTo("700.00");
        assertThat(result.fineTotal()).isEqualByComparingTo("50.00");
        assertThat(result.refundedTotal()).isEqualByComparingTo("120.00");
        // Líquido Realizado = 700 + 50 - 120 = 630
        assertThat(result.netRealizedTotal()).isEqualByComparingTo("630.00");
        // Previsto = 300 + 80 = 380
        assertThat(result.pendingTotal()).isEqualByComparingTo("380.00");
        // Locação líquido = 500 + 50 - 120 = 430; Venda líquido = 200
        assertThat(result.rentalNetTotal()).isEqualByComparingTo("430.00");
        assertThat(result.saleNetTotal()).isEqualByComparingTo("200.00");
        assertThat(result.days()).hasSize(31);
        assertThat(result.transactions()).hasSize(6);

        DailyMovementSummaryDTO summaryDay17 = result.days().get(16);
        assertThat(summaryDay17.date()).isEqualTo(day17);
        assertThat(summaryDay17.fineAmount()).isEqualByComparingTo("50.00");
        assertThat(summaryDay17.refundedAmount()).isEqualByComparingTo("120.00");
        assertThat(summaryDay17.netRealizedAmount()).isEqualByComparingTo("-70.00");
        assertThat(summaryDay17.pendingAmount()).isEqualByComparingTo("380.00");

        PaymentMethodBreakdownDTO cashBreakdown = result.byPaymentMethod().stream()
                .filter(b -> "CASH".equals(b.paymentMethod()))
                .findFirst()
                .orElseThrow();
        assertThat(cashBreakdown.fineAmount()).isEqualByComparingTo("50.00");
        assertThat(cashBreakdown.refundedAmount()).isEqualByComparingTo("120.00");
        assertThat(cashBreakdown.netRealizedAmount()).isEqualByComparingTo("-70.00");
    }

    @Test
    @DisplayName("Deve filtrar apenas RENTAL quando source=RENTAL")
    void shouldFilterOnlyRentalWhenSourceIsRental() {
        when(rentalPaymentRepository.findActiveMovementsBetween(any(), any())).thenReturn(List.of());

        MonthlyMovementResponseDTO result = service.getMonthlyMovement(2026, 5, "rental", null, null);

        assertThat(result.transactions()).isEmpty();
        verify(rentalPaymentRepository).findActiveMovementsBetween(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        verify(salesPaymentRepository, never()).findActiveMovementsBetween(any(), any());
    }

    @Test
    @DisplayName("Deve filtrar apenas SALE quando source=SALE")
    void shouldFilterOnlySaleWhenSourceIsSale() {
        SalesOrder walkInOrder = SalesOrder.builder()
                .id(UUID.randomUUID())
                .legacyId("")
                .customerName("")
                .status(SalesOrderStatus.PAID)
                .build();
        SalesPayment payment = SalesPayment.builder()
                .id(UUID.randomUUID())
                .salesOrder(walkInOrder)
                .installmentNumber(1)
                .installments(null)
                .paymentDate(LocalDate.of(2026, 5, 3))
                .paymentMethod(PaymentMethod.PIX)
                .status(PaymentStatus.PAID)
                .value(new BigDecimal("150.00"))
                .build();

        when(salesPaymentRepository.findActiveMovementsBetween(any(), any())).thenReturn(List.of(payment));

        MonthlyMovementResponseDTO result = service.getMonthlyMovement(2026, 5, "SALE", null, null);

        assertThat(result.transactions()).hasSize(1);
        assertThat(result.transactions().getFirst().customerName()).isEqualTo("Venda Balcão");
        verify(rentalPaymentRepository, never()).findActiveMovementsBetween(any(), any());
    }

    @Test
    @DisplayName("Deve consolidar breakdown por setor e aplicar filtro sectorId")
    void shouldBuildSectorBreakdownAndFilter() {
        UUID sectorA = UUID.randomUUID();
        UUID sectorB = UUID.randomUUID();

        RentalPayment inA = RentalPayment.builder()
                .id(UUID.randomUUID()).contract(contract)
                .installmentNumber(1).installments(1)
                .paymentDate(LocalDate.of(2026, 10, 10))
                .paymentMethod(PaymentMethod.PIX).status(PaymentStatus.PAID)
                .value(new BigDecimal("500.00")).sectorId(sectorA).build();
        RentalPayment inB = RentalPayment.builder()
                .id(UUID.randomUUID()).contract(contract)
                .installmentNumber(2).installments(1)
                .paymentDate(LocalDate.of(2026, 10, 10))
                .paymentMethod(PaymentMethod.CASH).status(PaymentStatus.PAID)
                .value(new BigDecimal("200.00")).sectorId(sectorB).build();

        when(rentalPaymentRepository.findActiveMovementsBetween(any(), any()))
                .thenReturn(List.of(inA, inB));
        when(sectorRepository.findAllById(any())).thenReturn(List.of(
                Sector.builder().id(sectorA).issuerCnpj("12345678000199")
                        .name("Balcão").code("BALCAO").build(),
                Sector.builder().id(sectorB).issuerCnpj("12345678000199")
                        .name("Depósito").code("DEPOSITO").build()));

        MonthlyMovementResponseDTO all = service.getMonthlyMovement(2026, 10, "ALL", null, null);

        assertThat(all.bySector()).hasSize(2);
        SectorMovementBreakdownDTO breakdownA = all.bySector().stream()
                .filter(s -> sectorA.equals(s.sectorId())).findFirst().orElseThrow();
        assertThat(breakdownA.sectorName()).isEqualTo("Balcão");
        assertThat(breakdownA.netRealizedAmount()).isEqualByComparingTo("500.00");

        MonthlyMovementResponseDTO filtered =
                service.getMonthlyMovement(2026, 10, "ALL", sectorA, null);

        assertThat(filtered.transactions()).hasSize(1);
        assertThat(filtered.transactions().getFirst().sectorId()).isEqualTo(sectorA);
        assertThat(filtered.netRealizedTotal()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Deve consolidar os 12 meses no movimento anual")
    void shouldConsolidateYearlyMovement() {
        RentalPayment janPayment = buildRentalPayment(
                LocalDate.of(2026, 1, 15), PaymentMethod.PIX, PaymentStatus.PAID, "1000.00", 1);
        SalesPayment octPayment = buildSalesPayment(
                LocalDate.of(2026, 10, 20), PaymentMethod.CREDIT_CARD, PaymentStatus.PENDING, "450.00", 1);

        when(rentalPaymentRepository.findActiveMovementsBetween(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(janPayment));
        when(salesPaymentRepository.findActiveMovementsBetween(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(octPayment));
        when(rentalPaymentRepository.findDistinctPaymentYears()).thenReturn(List.of(2026, 2025));
        when(salesPaymentRepository.findDistinctPaymentYears()).thenReturn(List.of(2026));

        YearlyMovementResponseDTO result = service.getYearlyMovement(2026, null, null, null);

        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.availableYears()).contains(2026, 2025);
        assertThat(result.netRealizedTotal()).isEqualByComparingTo("1000.00");
        assertThat(result.pendingTotal()).isEqualByComparingTo("450.00");
        assertThat(result.months()).hasSize(12);

        MonthlyBucketDTO janBucket = result.months().getFirst();
        assertThat(janBucket.month()).isEqualTo(1);
        assertThat(janBucket.netRealizedAmount()).isEqualByComparingTo("1000.00");

        MonthlyBucketDTO octBucket = result.months().get(9);
        assertThat(octBucket.month()).isEqualTo(10);
        assertThat(octBucket.pendingAmount()).isEqualByComparingTo("450.00");
    }

    @Test
    @DisplayName("Deve rejeitar mês, ano ou source inválidos com ValidationException")
    void shouldRejectInvalidParameters() {
        assertThatThrownBy(() -> service.getMonthlyMovement(2026, 13, "ALL", null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid month '13'");

        assertThatThrownBy(() -> service.getYearlyMovement(1990, "ALL", null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid year '1990'");

        assertThatThrownBy(() -> service.getMonthlyMovement(2026, 10, "INVALID", null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid movement source 'INVALID'");
    }

    private RentalPayment buildRentalPayment(
            LocalDate date, PaymentMethod method, PaymentStatus status, String value, int number) {
        return RentalPayment.builder()
                .id(UUID.randomUUID())
                .contract(contract)
                .installmentNumber(number)
                .installments(1)
                .paymentDate(date)
                .paymentMethod(method)
                .status(status)
                .value(new BigDecimal(value))
                .build();
    }

    private SalesPayment buildSalesPayment(
            LocalDate date, PaymentMethod method, PaymentStatus status, String value, int number) {
        return SalesPayment.builder()
                .id(UUID.randomUUID())
                .salesOrder(salesOrder)
                .installmentNumber(number)
                .installments(2)
                .paymentDate(date)
                .paymentMethod(method)
                .status(status)
                .value(new BigDecimal(value))
                .build();
    }
}
