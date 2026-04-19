package br.com.rentafit.rental.service;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.RentalPaymentDetailsDTO;
import br.com.rentafit.rental.dto.RentalPaymentInputDTO;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.repository.RentalPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import br.com.rentafit.rental.validation.RentalContractValidator;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RentalPaymentService - Unit Tests")
class RentalPaymentServiceTest {

    @Mock private RentalPaymentRepository paymentRepository;
    @Mock private RentalContractRepository contractRepository;
    @Mock private RentalContractValidator validator;
    @Mock private RentalMapper mapper;

    @InjectMocks
    private RentalPaymentService paymentService;

    private UUID contractId;
    private UUID paymentId;
    private RentalContract draftContract;
    private RentalContract finalizedContract;
    private RentalPayment existingPayment;
    private RentalPaymentInputDTO validPaymentDTO;
    private RentalPaymentDetailsDTO detailsDTO;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        paymentId  = UUID.randomUUID();

        RentalContractItem item = RentalContractItem.builder()
                .id(UUID.randomUUID())
                .description("Vestido")
                .value(new BigDecimal("500.00"))
                .metadata(new ArrayList<>())
                .build();

        draftContract = RentalContract.builder()
                .id(contractId)
                .status(ContractStatus.DRAFT)
                .eventDate(LocalDate.now().plusDays(30))
                .items(List.of(item))
                .payments(new ArrayList<>())
                .build();

        finalizedContract = RentalContract.builder()
                .id(contractId)
                .status(ContractStatus.FINALIZED)
                .eventDate(LocalDate.now().plusDays(30))
                .items(List.of(item))
                .payments(new ArrayList<>())
                .build();

        existingPayment = RentalPayment.builder()
                .id(paymentId)
                .contract(draftContract)
                .installmentNumber(1)
                .paymentDate(LocalDate.now().plusDays(10))
                .paymentMethod(PaymentMethod.PIX)
                .value(new BigDecimal("200.00"))
                .installments(1)
                .status(PaymentStatus.PENDING)
                .build();

        validPaymentDTO = new RentalPaymentInputDTO(
                1,
                LocalDate.now().plusDays(10),
                "PIX",
                new BigDecimal("200.00"),
                1, null, "PENDING"
        );

        detailsDTO = new RentalPaymentDetailsDTO(
                paymentId, 1, LocalDate.now().plusDays(10),
                "PIX", "PIX", new BigDecimal("200.00"), 1, null, "PENDING", "Pendente"
        );
    }

    // ── addPayment ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addPayment deve ser permitido mesmo em contrato FINALIZED")
    void testAddPayment_allowedAfterFinalized() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(0L);
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(BigDecimal.ZERO);
        when(mapper.toPaymentEntity(validPaymentDTO, finalizedContract)).thenReturn(existingPayment);
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);

        RentalPaymentDetailsDTO result = paymentService.addPayment(contractId, validPaymentDTO);

        assertThat(result).isNotNull();
        verify(paymentRepository).save(existingPayment);
    }

    @Test
    @DisplayName("addPayment deve lançar ValidationException se paymentDate > eventDate")
    void testAddPayment_dateAfterEventDate() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));

        RentalPaymentInputDTO lateDTO = new RentalPaymentInputDTO(
                1,
                LocalDate.now().plusDays(60), // depois do eventDate (30 dias)
                "PIX",
                new BigDecimal("100.00"),
                1, null, null
        );

        assertThatThrownBy(() -> paymentService.addPayment(contractId, lateDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("posterior à data do evento");
    }

    @Test
    @DisplayName("addPayment deve lançar ValidationException se paymentDate < hoje")
    void testAddPayment_dateBeforeToday() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));

        RentalPaymentInputDTO pastDTO = new RentalPaymentInputDTO(
                1,
                LocalDate.now().minusDays(1),
                "PIX",
                new BigDecimal("100.00"),
                1, null, null
        );

        assertThatThrownBy(() -> paymentService.addPayment(contractId, pastDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("anterior à data atual");

        verify(paymentRepository, never()).save(any(RentalPayment.class));
    }

    @Test
    @DisplayName("addPayment deve lançar ValidationException ao atingir 24 parcelas")
    void testAddPayment_exceeds24Installments() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(24L);

        assertThatThrownBy(() -> paymentService.addPayment(contractId, validPaymentDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("24");
    }

    @Test
    @DisplayName("addPayment deve lançar ValidationException se soma ultrapassa totalValue")
    void testAddPayment_exceedsTotalValue() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);
        // Committed = 400, new = 200, total items = 500 → 600 > 500
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(new BigDecimal("400.00"));

        assertThatThrownBy(() -> paymentService.addPayment(contractId, validPaymentDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ultrapassa");
    }

    // ── updatePayment ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updatePayment deve permitir alterar parcela não paga em contrato FINALIZED")
    void testUpdatePayment_allowsPendingWhenFinalized() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(new BigDecimal("200.00"));
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);
        // After save, sum matches total (500) → no deficit → no gap payment
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(contractId, paymentId, validPaymentDTO);

        assertThat(result).isNotNull().hasSize(1);
        verify(paymentRepository).save(existingPayment);
    }

    @Test
    @DisplayName("updatePayment deve bloquear alteração de parcela PAID após FINALIZED")
    void testUpdatePayment_paidBlockedWhenFinalized() {
        RentalPayment paidPayment = RentalPayment.builder()
                .id(paymentId)
                .contract(finalizedContract)
                .installmentNumber(1)
                .paymentDate(LocalDate.now().plusDays(10))
                .paymentMethod(PaymentMethod.PIX)
                .value(new BigDecimal("200.00"))
                .installments(1)
                .status(PaymentStatus.PAID)
                .processedByEmployeeId(UUID.randomUUID())
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(paidPayment));

        assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, validPaymentDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("parcela PAGA");
    }

    @Test
    @DisplayName("updatePayment deve atualizar parcela em contrato DRAFT")
    void testUpdatePayment_success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(new BigDecimal("200.00"));
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);
        // After save, sum matches total (500) → no deficit
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(contractId, paymentId, validPaymentDTO);

        assertThat(result).isNotNull().hasSize(1);
        verify(paymentRepository).save(existingPayment);
    }

    @Test
    @DisplayName("updatePayment deve lançar ValidationException se paymentDate < hoje")
    void testUpdatePayment_dateBeforeToday() {
        RentalPaymentInputDTO pastDTO = new RentalPaymentInputDTO(
                1,
                LocalDate.now().minusDays(1),
                "PIX",
                new BigDecimal("200.00"),
                1,
                null,
                "PENDING"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, pastDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("anterior à data atual");

        verify(paymentRepository, never()).save(any(RentalPayment.class));
    }

    // ── cancelPayment ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelPayment deve permitir cancelar parcela não paga em contrato FINALIZED")
    void testCancelPayment_allowsPendingWhenFinalized() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);

        paymentService.cancelPayment(contractId, paymentId);

        assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentRepository).save(existingPayment);
    }

    @Test
    @DisplayName("cancelPayment deve bloquear cancelamento de parcela PAID após SIGNED")
    void testCancelPayment_paidBlockedWhenSigned() {
        RentalContract signedContract = RentalContract.builder()
                .id(contractId)
                .status(ContractStatus.SIGNED)
                .eventDate(LocalDate.now().plusDays(30))
                .items(finalizedContract.getItems())
                .payments(new ArrayList<>())
                .build();

        RentalPayment paidPayment = RentalPayment.builder()
                .id(paymentId)
                .contract(signedContract)
                .installmentNumber(1)
                .paymentDate(LocalDate.now().plusDays(10))
                .paymentMethod(PaymentMethod.PIX)
                .value(new BigDecimal("200.00"))
                .installments(1)
                .status(PaymentStatus.PAID)
                .processedByEmployeeId(UUID.randomUUID())
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(signedContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(paidPayment));

        assertThatThrownBy(() -> paymentService.cancelPayment(contractId, paymentId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("parcela PAGA");
    }

    @Test
    @DisplayName("cancelPayment deve marcar parcela como CANCELLED")
    void testCancelPayment_success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);

        paymentService.cancelPayment(contractId, paymentId);

        assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentRepository).save(existingPayment);
    }

    // ── validateSinglePaidPaymentHasEmployee ──────────────────────────────────

    @Test
    @DisplayName("addPayment deve lançar ValidationException quando status=PAID e employeeId ausente")
    void testAddPayment_paidWithoutEmployee_throwsValidationException() {
        RentalPaymentInputDTO paidWithoutEmployee = new RentalPaymentInputDTO(
                1,
                LocalDate.now().plusDays(5),
                "PIX",
                new BigDecimal("200.00"),
                1,
                null,   // processedByEmployeeId ausente
                "PAID"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(0L);
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(BigDecimal.ZERO);
        doThrow(new ValidationException("processedByEmployeeId"))
                .when(validator).validateSinglePaidPaymentHasEmployee(paidWithoutEmployee);

        assertThatThrownBy(() -> paymentService.addPayment(contractId, paidWithoutEmployee))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("processedByEmployeeId");
    }

    @Test
    @DisplayName("addPayment deve ser permitido quando status=PAID e employeeId informado")
    void testAddPayment_paidWithEmployee_success() {
        UUID employeeId = UUID.randomUUID();
        RentalPaymentInputDTO paidWithEmployee = new RentalPaymentInputDTO(
                1,
                LocalDate.now().plusDays(5),
                "PIX",
                new BigDecimal("200.00"),
                1,
                employeeId,
                "PAID"
        );

        RentalPayment savedPayment = RentalPayment.builder()
                .id(paymentId).contract(draftContract).installmentNumber(1)
                .paymentDate(LocalDate.now().plusDays(5)).paymentMethod(PaymentMethod.PIX)
                .value(new BigDecimal("200.00")).installments(1)
                .processedByEmployeeId(employeeId).status(PaymentStatus.PAID).build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(0L);
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(BigDecimal.ZERO);
        doNothing().when(validator).validateSinglePaidPaymentHasEmployee(paidWithEmployee);
        when(mapper.toPaymentEntity(paidWithEmployee, draftContract)).thenReturn(savedPayment);
        when(paymentRepository.save(savedPayment)).thenReturn(savedPayment);
        when(mapper.toPaymentDetailsDTO(savedPayment)).thenReturn(detailsDTO);

        RentalPaymentDetailsDTO result = paymentService.addPayment(contractId, paidWithEmployee);

        assertThat(result).isNotNull();
        verify(validator).validateSinglePaidPaymentHasEmployee(paidWithEmployee);
        verify(paymentRepository).save(savedPayment);
    }

    @Test
    @DisplayName("updatePayment deve lançar ValidationException quando status=PAID e employeeId ausente")
    void testUpdatePayment_paidWithoutEmployee_throwsValidationException() {
        RentalPaymentInputDTO paidWithoutEmployee = new RentalPaymentInputDTO(
                1,
                LocalDate.now().plusDays(5),
                "PIX",
                new BigDecimal("200.00"),
                1,
                null,   // processedByEmployeeId ausente
                "PAID"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(new BigDecimal("200.00"));
        doThrow(new ValidationException("processedByEmployeeId"))
                .when(validator).validateSinglePaidPaymentHasEmployee(paidWithoutEmployee);

        assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, paidWithoutEmployee))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("processedByEmployeeId");
    }

    @Test
    @DisplayName("updatePayment deve ser permitido quando status=PAID e employeeId informado")
    void testUpdatePayment_paidWithEmployee_success() {
        UUID employeeId = UUID.randomUUID();
        RentalPaymentInputDTO paidWithEmployee = new RentalPaymentInputDTO(
                1,
                LocalDate.now().plusDays(5),
                "PIX",
                new BigDecimal("200.00"),
                1,
                employeeId,
                "PAID"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(new BigDecimal("200.00"));
        doNothing().when(validator).validateSinglePaidPaymentHasEmployee(paidWithEmployee);
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);
        // After save, sum matches total (500) → no deficit
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(contractId, paymentId, paidWithEmployee);

        assertThat(result).isNotNull().hasSize(1);
        verify(validator).validateSinglePaidPaymentHasEmployee(paidWithEmployee);
    }

    // ── auto-gap payment on updatePayment ─────────────────────────────────────

    @Test
    @DisplayName("updatePayment deve criar parcela-gap quando redução de valor gera déficit")
    void testUpdatePayment_createsGapPayment_whenValueReduced() {
        // Contract total = 500. Existing payment = 200 PENDING.
        // Reduce to 100 → committed = 100, deficit = 400 → gap payment of 400 created.
        RentalPaymentInputDTO reducedDTO = new RentalPaymentInputDTO(
                1, LocalDate.now().plusDays(10), "PIX",
                new BigDecimal("100.00"), 1, null, "PENDING"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        // validateTotalValueNotExceeded: sum before = 200, subtract existing 200, add new 100 = 100 < 500 → OK
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(new BigDecimal("200.00"))   // first call: validateTotalValueNotExceeded
                .thenReturn(new BigDecimal("100.00"));  // second call: autoCreateGapPaymentIfNeeded (after save, sum is 100)

        // Auto-gap: deficit = 500 - 100 = 400
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);
        when(paymentRepository.findMaxInstallmentNumberByContractId(contractId)).thenReturn(1);
        when(paymentRepository.findMaxPaymentDateByContractId(contractId))
                .thenReturn(Optional.of(LocalDate.now().plusDays(10)));

        RentalPayment gapPayment = RentalPayment.builder()
                .id(UUID.randomUUID())
                .contract(draftContract)
                .installmentNumber(2)
                .paymentDate(LocalDate.now().plusDays(30))
                .paymentMethod(PaymentMethod.PIX)
                .value(new BigDecimal("400.00"))
                .installments(1)
                .status(PaymentStatus.PENDING)
                .build();

        RentalPaymentDetailsDTO gapDetailsDTO = new RentalPaymentDetailsDTO(
                gapPayment.getId(), 2, gapPayment.getPaymentDate(),
                "PIX", "PIX", new BigDecimal("400.00"), 1, null, "PENDING", "Pendente"
        );

        when(paymentRepository.save(any(RentalPayment.class))).thenReturn(existingPayment, gapPayment);
        when(mapper.toPaymentDetailsDTO(any(RentalPayment.class))).thenReturn(detailsDTO, gapDetailsDTO);

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(contractId, paymentId, reducedDTO);

        assertThat(result).hasSize(2);
        // save called twice: once for the updated payment, once for the gap
        verify(paymentRepository, times(2)).save(any(RentalPayment.class));
    }

    @Test
    @DisplayName("updatePayment NÃO deve criar parcela-gap quando valor não gera déficit")
    void testUpdatePayment_noGapPayment_whenNoDeficit() {
        // Contract total = 500. Existing payment = 200. Update to 500 → no deficit.
        RentalPaymentInputDTO fullDTO = new RentalPaymentInputDTO(
                1, LocalDate.now().plusDays(10), "PIX",
                new BigDecimal("500.00"), 1, null, "PENDING"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(new BigDecimal("200.00"))   // validateTotalValueNotExceeded
                .thenReturn(new BigDecimal("500.00"));  // autoCreateGapPaymentIfNeeded: 500 == 500 → no deficit
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(contractId, paymentId, fullDTO);

        assertThat(result).hasSize(1);
        // save called only once: for the updated payment
        verify(paymentRepository, times(1)).save(any(RentalPayment.class));
    }

    @Test
    @DisplayName("updatePayment deve criar parcela-gap ao marcar PENDING como PAID com valor menor")
    void testUpdatePayment_createsGapPayment_whenMarkedPaidWithLowerValue() {
        // Contract total = 500. Existing payment = 200 PENDING.
        // Mark as PAID with value = 50 → committed = 50, deficit = 450 → gap of 450 created.
        UUID employeeId = UUID.randomUUID();
        RentalPaymentInputDTO paidReducedDTO = new RentalPaymentInputDTO(
                1, LocalDate.now().plusDays(10), "PIX",
                new BigDecimal("50.00"), 1, employeeId, "PAID"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(new BigDecimal("200.00"))  // validateTotalValueNotExceeded
                .thenReturn(new BigDecimal("50.00"));  // autoCreateGapPaymentIfNeeded: deficit = 500-50 = 450
        when(paymentRepository.save(any(RentalPayment.class))).thenAnswer(i -> i.getArgument(0));

        RentalPaymentDetailsDTO updatedDTO = new RentalPaymentDetailsDTO(
                paymentId, 1, LocalDate.now().plusDays(10),
                "PIX", "PIX", new BigDecimal("50.00"), 1, employeeId, "PAID", "Pago"
        );
        RentalPaymentDetailsDTO gapDTO = new RentalPaymentDetailsDTO(
                UUID.randomUUID(), 2, LocalDate.now().plusDays(30),
                "PIX", "PIX", new BigDecimal("450.00"), 1, null, "PENDING", "Pendente"
        );
        when(mapper.toPaymentDetailsDTO(any(RentalPayment.class))).thenReturn(updatedDTO, gapDTO);
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);
        when(paymentRepository.findMaxInstallmentNumberByContractId(contractId)).thenReturn(1);
        when(paymentRepository.findMaxPaymentDateByContractId(contractId))
                .thenReturn(Optional.of(LocalDate.now().plusDays(10)));

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(contractId, paymentId, paidReducedDTO);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).status()).isEqualTo("PAID");
        assertThat(result.get(1).status()).isEqualTo("PENDING");
        assertThat(result.get(1).value()).isEqualByComparingTo("450.00");
        verify(paymentRepository, times(2)).save(any(RentalPayment.class));
    }

    @Test
    @DisplayName("BUG REGRESSION: cenário exato do HAR - legacyId=1 - parcela PENDING reduzida e marcada PAID deve gerar gap")
    void testUpdatePayment_harBugRegression_contract1() {
        LocalDate existingPaymentDate = LocalDate.now().plusDays(1);
        LocalDate eventDate = LocalDate.now().plusDays(30);

        // Setup: contract total = 590, payments: #1=290/PAID, #2=200/PENDING, #3=100/PENDING
        // Action: PUT #3 with value=50, status=PAID
        // Expected: gap payment #4=50/PENDING is auto-created
        UUID harContractId = UUID.randomUUID();
        UUID harPaymentId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        RentalContractItem smokingSlim = RentalContractItem.builder()
                .id(UUID.randomUUID())
                .description("Smoking Slim")
                .value(new BigDecimal("590.00"))
                .metadata(new ArrayList<>())
                .build();

        RentalContract harContract = RentalContract.builder()
                .id(harContractId)
                .status(ContractStatus.FINALIZED)
                .eventDate(eventDate)
                .items(List.of(smokingSlim))
                .payments(new ArrayList<>())
                .build();

        RentalPayment pendingPayment3 = RentalPayment.builder()
                .id(harPaymentId)
                .contract(harContract)
                .installmentNumber(3)
                .paymentDate(existingPaymentDate)
                .paymentMethod(PaymentMethod.PIX)
                .value(new BigDecimal("100.00"))
                .installments(1)
                .status(PaymentStatus.PENDING)
                .build();

        RentalPaymentInputDTO putDTO = new RentalPaymentInputDTO(
                3,
                existingPaymentDate,
                "PIX",
                new BigDecimal("50.00"),
                1,
                employeeId,
                "PAID"
        );

        when(contractRepository.findById(harContractId)).thenReturn(Optional.of(harContract));
        when(paymentRepository.findByIdAndContractId(harPaymentId, harContractId)).thenReturn(Optional.of(pendingPayment3));
        // validateTotalValueNotExceeded: sum before = 290+200+100=590, subtract existing 100, add new 50 = 540 < 590 → OK
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(harContractId), any()))
                .thenReturn(new BigDecimal("590.00"))  // validateTotalValueNotExceeded (before update)
                .thenReturn(new BigDecimal("540.00")); // autoCreateGapPaymentIfNeeded: 590-540 = 50 deficit
        when(paymentRepository.save(any(RentalPayment.class))).thenAnswer(i -> i.getArgument(0));

        RentalPaymentDetailsDTO updatedDetail = new RentalPaymentDetailsDTO(
                harPaymentId, 3, existingPaymentDate,
                "PIX", "PIX", new BigDecimal("50.00"), 1, employeeId, "PAID", "Pago"
        );
        RentalPaymentDetailsDTO gapDetail = new RentalPaymentDetailsDTO(
                UUID.randomUUID(), 4, eventDate,
                "PIX", "PIX", new BigDecimal("50.00"), 1, null, "PENDING", "Pendente"
        );
        when(mapper.toPaymentDetailsDTO(any(RentalPayment.class))).thenReturn(updatedDetail, gapDetail);

        // Auto-gap checks
        when(paymentRepository.countByContractIdAndStatusNot(harContractId, PaymentStatus.CANCELLED)).thenReturn(3L);
        when(paymentRepository.findMaxInstallmentNumberByContractId(harContractId)).thenReturn(3);
        // maxPaymentDate among existing: 2026-04-27 (payment #2 in original) — but here we simplify
        when(paymentRepository.findMaxPaymentDateByContractId(harContractId))
                .thenReturn(Optional.of(existingPaymentDate));

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(harContractId, harPaymentId, putDTO);

        // Must return 2 entries: the updated payment + the auto-gap
        assertThat(result).hasSize(2);

        // First element: the updated payment #3 → 50/PAID
        assertThat(result.get(0).installmentNumber()).isEqualTo(3);
        assertThat(result.get(0).value()).isEqualByComparingTo("50.00");
        assertThat(result.get(0).status()).isEqualTo("PAID");

        // Second element: the auto-gap payment #4 → 50/PENDING
        assertThat(result.get(1).installmentNumber()).isEqualTo(4);
        assertThat(result.get(1).value()).isEqualByComparingTo("50.00");
        assertThat(result.get(1).status()).isEqualTo("PENDING");

        // Two saves: updated payment + gap payment
        verify(paymentRepository, times(2)).save(any(RentalPayment.class));
    }
}

