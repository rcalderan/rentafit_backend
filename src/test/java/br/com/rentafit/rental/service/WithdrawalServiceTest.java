package br.com.rentafit.rental.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.repository.EmployeeRepository;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.ItemMetaType;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.WithdrawContractDTO;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.repository.RentalPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawalService - Desistência de contrato")
class WithdrawalServiceTest {

    @Mock private RentalContractRepository contractRepository;
    @Mock private RentalPaymentRepository paymentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private RentalWorkflowService workflowService;
    @Mock private RentalMapper mapper;

    @InjectMocks
    private WithdrawalService withdrawalService;

    private UUID contractId;
    private UUID employeeId;
    private RentalContract contract;
    private RentalContractItem item;
    private RentalPayment paidPayment;
    private RentalPayment pendingPayment;
    private Employee employee;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        employee = mock(Employee.class);
        lenient().when(employee.getName()).thenReturn("Richard");

        item = RentalContractItem.builder()
                .id(UUID.randomUUID())
                .rentalItemId(UUID.randomUUID())
                .description("Terno Slim")
                .value(BigDecimal.valueOf(450))
                .delivered(false)
                .returned(false)
                .metadata(new ArrayList<>())
                .build();

        paidPayment = RentalPayment.builder()
                .id(UUID.randomUUID())
                .installmentNumber(1)
                .paymentDate(LocalDate.now().minusDays(5))
                .paymentMethod(PaymentMethod.PIX)
                .value(BigDecimal.valueOf(300))
                .installments(1)
                .status(PaymentStatus.PAID)
                .build();

        pendingPayment = RentalPayment.builder()
                .id(UUID.randomUUID())
                .installmentNumber(2)
                .paymentDate(LocalDate.now().plusDays(10))
                .paymentMethod(PaymentMethod.PIX)
                .value(BigDecimal.valueOf(150))
                .installments(1)
                .status(PaymentStatus.PENDING)
                .build();

        List<RentalContractItem> items = new ArrayList<>();
        items.add(item);
        List<RentalPayment> payments = new ArrayList<>();
        payments.add(paidPayment);
        payments.add(pendingPayment);

        contract = RentalContract.builder()
                .id(contractId)
                .legacyId("20261006-1")
                .customerName("Maria Silva")
                .customerId(UUID.randomUUID())
                .createdByEmployeeId(employeeId)
                .status(ContractStatus.FINALIZED)
                .pickupDate(LocalDate.now().plusDays(3))
                .eventDate(LocalDate.now().plusDays(5))
                .returnDate(LocalDate.now().plusDays(7))
                .returned(false)
                .items(items)
                .payments(payments)
                .build();

        item.setContract(contract);
        paidPayment.setContract(contract);
        pendingPayment.setContract(contract);
    }

    private WithdrawContractDTO dto(BigDecimal refundAmount, boolean applyFine, BigDecimal fineAmount) {
        return new WithdrawContractDTO(employeeId, refundAmount, applyFine, fineAmount);
    }

    private RentalPayment savedPaymentWithStatus(PaymentStatus status) {
        ArgumentCaptor<RentalPayment> captor = ArgumentCaptor.forClass(RentalPayment.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(p -> status.equals(p.getStatus()))
                .findFirst().orElseThrow();
    }

    private void stubHappyPath() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        lenient().when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        lenient().when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("desistência de FINALIZED: cancela contrato, registra reembolso e dispara workflow")
    void shouldWithdrawFinalizedContract() {
        stubHappyPath();
        when(paymentRepository.findMaxInstallmentNumberByContractId(contractId)).thenReturn(2);
        WithdrawContractDTO request = dto(BigDecimal.valueOf(300), false, null);

        withdrawalService.withdraw(contractId, request);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.CANCELLED);
        assertThat(contract.getReturned()).isTrue();
        assertThat(contract.getActualReturnDate()).isEqualTo(LocalDate.now());
        assertThat(contract.getReturnedByEmployeeId()).isEqualTo(employeeId);
        assertThat(paidPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        RentalPayment refund = savedPaymentWithStatus(PaymentStatus.REFUNDED);
        assertThat(refund.getValue()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(refund.getInstallmentNumber()).isEqualTo(3);
        assertThat(refund.getProcessedByEmployeeId()).isEqualTo(employeeId);
        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(item.getReturned()).isTrue();
        assertThat(item.getReturnedByName()).contains("Desistência");
        verify(workflowService).onWithdraw(contract);
        verify(mapper).toDetailsDTO(contract, null);
    }

    @Test
    @DisplayName("desistência de SIGNED: não dispara workflow de estoque")
    void shouldNotTouchStockForSignedContract() {
        contract.setStatus(ContractStatus.SIGNED);
        stubHappyPath();

        withdrawalService.withdraw(contractId, dto(null, false, null));

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.CANCELLED);
        verify(workflowService, never()).onWithdraw(any());
        verify(workflowService, never()).onReturn(any());
    }

    @Test
    @DisplayName("sem seleção de reembolso: parcela PAID permanece PAID")
    void shouldKeepPaidWhenNoRefundSelected() {
        stubHappyPath();

        withdrawalService.withdraw(contractId, dto(null, false, null));

        assertThat(paidPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("reembolso parcial: valor arbitrário gera lançamento REFUNDED")
    void shouldRecordPartialRefundAmount() {
        stubHappyPath();
        when(paymentRepository.findMaxInstallmentNumberByContractId(contractId)).thenReturn(2);

        withdrawalService.withdraw(contractId, dto(BigDecimal.valueOf(50), false, null));

        RentalPayment refund = savedPaymentWithStatus(PaymentStatus.REFUNDED);
        assertThat(refund.getValue()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(paidPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("rejeita refundAmount maior que o total pago")
    void shouldRejectRefundAbovePaidTotal() {
        stubHappyPath();

        assertThatThrownBy(() -> withdrawalService.withdraw(
                contractId, dto(BigDecimal.valueOf(300.01), false, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("300.01");

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.FINALIZED);
        verify(workflowService, never()).onWithdraw(any());
    }

    @Test
    @DisplayName("reembolso igual ao total pago é aceito")
    void shouldAcceptRefundEqualToPaidTotal() {
        stubHappyPath();
        when(paymentRepository.findMaxInstallmentNumberByContractId(contractId)).thenReturn(2);

        withdrawalService.withdraw(contractId, dto(BigDecimal.valueOf(300), false, null));

        assertThat(savedPaymentWithStatus(PaymentStatus.REFUNDED).getValue())
                .isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    @DisplayName("applyFine=true cria parcela MULTA com próximo installmentNumber")
    void shouldCreateRescissionFine() {
        stubHappyPath();
        when(paymentRepository.findMaxInstallmentNumberByContractId(contractId)).thenReturn(2);
        BigDecimal fine = BigDecimal.valueOf(135);

        withdrawalService.withdraw(contractId, dto(null, true, fine));

        ArgumentCaptor<RentalPayment> captor = ArgumentCaptor.forClass(RentalPayment.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());
        RentalPayment finePayment = captor.getAllValues().stream()
                .filter(p -> PaymentStatus.MULTA.equals(p.getStatus()))
                .findFirst().orElseThrow();
        assertThat(finePayment.getValue()).isEqualByComparingTo(fine);
        assertThat(finePayment.getInstallmentNumber()).isEqualTo(3);
        assertThat(finePayment.getProcessedByEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    @DisplayName("applyFine=false não cria parcela MULTA")
    void shouldNotCreateFineWhenNotApplied() {
        stubHappyPath();

        withdrawalService.withdraw(contractId, dto(null, false, BigDecimal.valueOf(135)));

        verify(paymentRepository, never()).findMaxInstallmentNumberByContractId(any());
        assertThat(contract.getPayments()).noneMatch(p -> PaymentStatus.MULTA.equals(p.getStatus()));
    }

    @Test
    @DisplayName("rejeita desistência de contrato CLOSED")
    void shouldRejectClosedContract() {
        contract.setStatus(ContractStatus.CLOSED);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> withdrawalService.withdraw(contractId, dto(null, false, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("SIGNED ou FINALIZED");
    }

    @Test
    @DisplayName("rejeita desistência de contrato DRAFT")
    void shouldRejectDraftContract() {
        contract.setStatus(ContractStatus.DRAFT);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> withdrawalService.withdraw(contractId, dto(null, false, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("lança 404 para contrato inexistente")
    void shouldThrowNotFoundForMissingContract() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalService.withdraw(contractId, dto(null, false, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("lança 404 para funcionário inexistente")
    void shouldThrowNotFoundForMissingEmployee() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalService.withdraw(contractId, dto(null, false, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(employeeId.toString());
    }

    @Test
    @DisplayName("marca acessório pendente como devolvido na desistência")
    void shouldMarkAccessoriesReturned() {
        RentalContractItemMeta accessory = RentalContractItemMeta.builder()
                .id(UUID.randomUUID())
                .contractItem(item)
                .type(ItemMetaType.ACESSORIO)
                .description("Gravata")
                .accessoryId(UUID.randomUUID())
                .returned(false)
                .build();
        item.getMetadata().add(accessory);
        stubHappyPath();

        withdrawalService.withdraw(contractId, dto(null, false, null));

        assertThat(accessory.getReturned()).isTrue();
        assertThat(accessory.getReturnedAt()).isNotNull();
    }
}
