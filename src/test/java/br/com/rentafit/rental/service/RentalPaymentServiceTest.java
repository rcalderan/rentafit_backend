package br.com.rentafit.rental.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
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
    @DisplayName("BUG-2026-05-10-3 REGRESSION: updatePayment deve persistir dto.paymentDate(), não LocalDate.now()")
    void testUpdatePayment_bug3_mustPersistDtoPaymentDate() {
        LocalDate futureDate = LocalDate.now().plusDays(15);
        RentalPaymentInputDTO dtoWithFutureDate = new RentalPaymentInputDTO(
                1, futureDate, "PIX", new BigDecimal("200.00"), 1, null, "PENDING");

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(new BigDecimal("200.00"));
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);

        paymentService.updatePayment(contractId, paymentId, dtoWithFutureDate);

        // BUG-3: o código tinha payment.setPaymentDate(today) em vez de payment.setPaymentDate(dto.paymentDate())
        // Esperamos que a data persistida seja a do DTO, não a data de hoje
        assertThat(existingPayment.getPaymentDate())
                .as("BUG-3: paymentDate deve ser persistida com o valor do DTO, não LocalDate.now()")
                .isEqualTo(futureDate);
    }

    @Test
    @DisplayName("BUG REGRESSION: cenário do HAR deve bloquear baixa com alteração financeira em contrato FINALIZED")
    void testUpdatePayment_harBugRegression_contract1() {
        LocalDate existingPaymentDate = LocalDate.now().plusDays(1);
        LocalDate eventDate = LocalDate.now().plusDays(30);

        // Setup: contract total = 590, payments: #1=290/PAID, #2=200/PENDING, #3=100/PENDING
        // Action: PUT #3 with value=50, status=PAID (baixa + alteração financeira no mesmo request)
        // Expected: bloqueado por regra de integridade para contratos FINALIZED
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
        assertThatThrownBy(() -> paymentService.updatePayment(harContractId, harPaymentId, putDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("não é permitido alterar número, data, forma ou valor");

        verify(paymentRepository, never()).save(any(RentalPayment.class));
    }

    // ── Additional Coverage Tests ─────────────────────────────────────────────

    @Test
    @DisplayName("listByContract deve retornar lista de parcelas quando contrato existe")
    void testListByContract_success() {
        when(contractRepository.existsById(contractId)).thenReturn(true);
        when(paymentRepository.findByContractIdOrderByInstallmentNumber(contractId))
                .thenReturn(List.of(existingPayment));
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);

        List<RentalPaymentDetailsDTO> result = paymentService.listByContract(contractId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(detailsDTO);
    }

    @Test
    @DisplayName("listByContract deve lancar ResourceNotFoundException quando contrato nao existe")
    void testListByContract_contractNotFound() {
        when(contractRepository.existsById(contractId)).thenReturn(false);

        assertThatThrownBy(() -> paymentService.listByContract(contractId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RentalContract")
                .hasMessageContaining(contractId.toString());
    }

    @Test
    @DisplayName("addPayment deve lancar ResourceNotFoundException quando contrato nao existe")
    void testAddPayment_contractNotFound() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.addPayment(contractId, validPaymentDTO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updatePayment deve lancar ResourceNotFoundException quando contrato nao existe")
    void testUpdatePayment_contractNotFound() {
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, validPaymentDTO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updatePayment deve lancar ResourceNotFoundException quando parcela nao existe")
    void testUpdatePayment_paymentNotFound() {
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, validPaymentDTO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cancelPayment deve lancar ResourceNotFoundException quando contrato nao existe")
    void testCancelPayment_contractNotFound() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.cancelPayment(contractId, paymentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cancelPayment deve lancar ResourceNotFoundException quando parcela nao existe")
    void testCancelPayment_paymentNotFound() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.cancelPayment(contractId, paymentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("validatePaymentDate deve aceitar paymentDate nula")
    void testValidatePaymentDate_nullDate() {
        RentalPaymentInputDTO nullDateDTO = new RentalPaymentInputDTO(
                1, null, "PIX", new BigDecimal("200.00"), 1, null, "PENDING"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(0L);
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(BigDecimal.ZERO);
        when(mapper.toPaymentEntity(nullDateDTO, draftContract)).thenReturn(existingPayment);
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);

        RentalPaymentDetailsDTO result = paymentService.addPayment(contractId, nullDateDTO);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("validatePaymentDate deve aceitar contract eventDate nula")
    void testValidatePaymentDate_nullContractEventDate() {
        RentalContract nullEventContract = RentalContract.builder()
                .id(contractId)
                .status(ContractStatus.DRAFT)
                .eventDate(null)
                .items(draftContract.getItems())
                .payments(new ArrayList<>())
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(nullEventContract));
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(0L);
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(BigDecimal.ZERO);
        when(mapper.toPaymentEntity(validPaymentDTO, nullEventContract)).thenReturn(existingPayment);
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);

        RentalPaymentDetailsDTO result = paymentService.addPayment(contractId, validPaymentDTO);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("validateTotalValueNotExceeded nao deve subtrair quando payment status nao for PENDING ou PAID")
    void testValidateTotalValueNotExceeded_cancelledPaymentNotSubtracted() {
        RentalPayment cancelledPayment = RentalPayment.builder()
                .id(paymentId)
                .contract(draftContract)
                .installmentNumber(1)
                .paymentDate(LocalDate.now().plusDays(10))
                .paymentMethod(PaymentMethod.PIX)
                .value(new BigDecimal("200.00"))
                .installments(1)
                .status(PaymentStatus.CANCELLED)
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(cancelledPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(new BigDecimal("400.00"));

        assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, validPaymentDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ultrapassa");
    }

    @Test
    @DisplayName("validateTotalValueNotExceeded nao deve subtrair quando payment value for nulo")
    void testValidateTotalValueNotExceeded_nullPaymentValueNotSubtracted() {
        RentalPayment nullValuePayment = RentalPayment.builder()
                .id(paymentId)
                .contract(draftContract)
                .installmentNumber(1)
                .paymentDate(LocalDate.now().plusDays(10))
                .paymentMethod(PaymentMethod.PIX)
                .value(null)
                .installments(1)
                .status(PaymentStatus.PENDING)
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(nullValuePayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(new BigDecimal("400.00"));

        assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, validPaymentDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ultrapassa");
    }

    @Test
    @DisplayName("validateTotalValueNotExceeded deve aceitar newValue nulo")
    void testValidateTotalValueNotExceeded_nullNewValue() {
        RentalPaymentInputDTO nullValueDTO = new RentalPaymentInputDTO(
                1, LocalDate.now().plusDays(10), "PIX", null, 1, null, "PENDING"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(new BigDecimal("200.00"));
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(contractId, paymentId, nullValueDTO);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("validatePaidInstallmentMutationAllowed deve bloquear alteracao de PAID em contrato SIGNED/REVISION/CLOSED")
    void testValidatePaidInstallmentMutationAllowed_variousLockedStatuses() {
        for (ContractStatus status : List.of(ContractStatus.SIGNED, ContractStatus.REVISION, ContractStatus.CLOSED)) {
            RentalContract lockedContract = RentalContract.builder()
                    .id(contractId)
                    .status(status)
                    .eventDate(LocalDate.now().plusDays(30))
                    .items(draftContract.getItems())
                    .payments(new ArrayList<>())
                    .build();

            RentalPayment paidPayment = RentalPayment.builder()
                    .id(paymentId)
                    .contract(lockedContract)
                    .installmentNumber(1)
                    .paymentDate(LocalDate.now().plusDays(10))
                    .paymentMethod(PaymentMethod.PIX)
                    .value(new BigDecimal("200.00"))
                    .installments(1)
                    .status(PaymentStatus.PAID)
                    .processedByEmployeeId(UUID.randomUUID())
                    .build();

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(lockedContract));
            when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(paidPayment));

            assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, validPaymentDTO))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("parcela PAGA");
        }
    }

    @Test
    @DisplayName("validateLockedContractSettlementIntegrity deve aceitar update quando contract nao esta locked")
    void testValidateLockedContractSettlementIntegrity_notLocked() {
        UUID employeeId = UUID.randomUUID();
        RentalPaymentInputDTO paidChangedDTO = new RentalPaymentInputDTO(
                1, LocalDate.now().plusDays(15), "PIX", new BigDecimal("300.00"), 1, employeeId, "PAID"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(new BigDecimal("200.00"));
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(contractId, paymentId, paidChangedDTO);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("validateLockedContractSettlementIntegrity deve aceitar update quando nao esta baixando agora")
    void testValidateLockedContractSettlementIntegrity_notSettlingNow() {
        RentalPaymentInputDTO pendingChangedDTO = new RentalPaymentInputDTO(
                1, LocalDate.now().plusDays(15), "PIX", new BigDecimal("100.00"), 1, null, "PENDING"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(new BigDecimal("200.00"));
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(contractId, paymentId, pendingChangedDTO);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("validateLockedContractSettlementIntegrity deve lancar excecao quando numero da parcela muda")
    void testValidateLockedContractSettlementIntegrity_installmentChanged() {
        RentalPaymentInputDTO badDTO = new RentalPaymentInputDTO(
                9,
                existingPayment.getPaymentDate(), "PIX", existingPayment.getValue(), 1, UUID.randomUUID(), "PAID"
        );
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, badDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("não é permitido alterar número, data, forma ou valor");
    }

    @Test
    @DisplayName("validateLockedContractSettlementIntegrity deve lancar excecao quando data da parcela muda")
    void testValidateLockedContractSettlementIntegrity_dateChanged() {
        RentalPaymentInputDTO badDTO = new RentalPaymentInputDTO(
                1,
                existingPayment.getPaymentDate().plusDays(1),
                "PIX", existingPayment.getValue(), 1, UUID.randomUUID(), "PAID"
        );
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, badDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("não é permitido alterar número, data, forma ou valor");
    }

    @Test
    @DisplayName("validateLockedContractSettlementIntegrity deve lancar excecao quando metodo muda")
    void testValidateLockedContractSettlementIntegrity_methodChanged() {
        RentalPaymentInputDTO badDTO = new RentalPaymentInputDTO(
                1,
                existingPayment.getPaymentDate(),
                "CREDIT_CARD",
                existingPayment.getValue(), 1, UUID.randomUUID(), "PAID"
        );
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, badDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("não é permitido alterar número, data, forma ou valor");
    }

    @Test
    @DisplayName("autoCreateGapPaymentIfNeeded nao deve criar parcela-gap quando activeCount >= 24")
    void testAutoCreateGapPaymentIfNeeded_maxInstallmentsReached() {
        RentalPaymentInputDTO reducedDTO = new RentalPaymentInputDTO(
                1, LocalDate.now().plusDays(10), "PIX", new BigDecimal("100.00"), 1, null, "PENDING"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(new BigDecimal("200.00"))
                .thenReturn(new BigDecimal("100.00"));
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(24L);
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(contractId, paymentId, reducedDTO);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("autoCreateGapPaymentIfNeeded deve ignorar itens com valor nulo no calculo do total")
    void testAutoCreateGapPaymentIfNeeded_nullValueItem() {
        RentalContractItem nullValueItem = RentalContractItem.builder()
                .id(UUID.randomUUID())
                .description("Acessorio")
                .value(null)
                .metadata(new ArrayList<>())
                .build();

        RentalContract nullValueContract = RentalContract.builder()
                .id(contractId)
                .status(ContractStatus.DRAFT)
                .eventDate(LocalDate.now().plusDays(30))
                .items(List.of(nullValueItem))
                .payments(new ArrayList<>())
                .build();

        RentalPaymentInputDTO reducedDTO = new RentalPaymentInputDTO(
                1, LocalDate.now().plusDays(10), "PIX", new BigDecimal("0.00"), 1, null, "PENDING"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(nullValueContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(new BigDecimal("200.00"))
                .thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);

        List<RentalPaymentDetailsDTO> result = paymentService.updatePayment(contractId, paymentId, reducedDTO);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("computeNextPaymentDate deve usar eventDate quando latestPaymentDate for nulo")
    void testComputeNextPaymentDate_nullLatestPaymentDate() {
        RentalPaymentInputDTO reducedDTO = new RentalPaymentInputDTO(
                1, LocalDate.now().plusDays(10), "PIX", new BigDecimal("100.00"), 1, null, "PENDING"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(new BigDecimal("200.00"))
                .thenReturn(new BigDecimal("100.00"));
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);
        when(paymentRepository.findMaxInstallmentNumberByContractId(contractId)).thenReturn(1);
        when(paymentRepository.findMaxPaymentDateByContractId(contractId)).thenReturn(Optional.empty());

        RentalPayment gapPayment = RentalPayment.builder()
                .id(UUID.randomUUID())
                .contract(draftContract)
                .installmentNumber(2)
                .paymentDate(draftContract.getEventDate())
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
        assertThat(result.get(1).paymentDate()).isEqualTo(draftContract.getEventDate());
    }

    @Test
    @DisplayName("computeNextPaymentDate deve retornar candidate quando candidate <= eventDate")
    void testComputeNextPaymentDate_candidateBeforeOrEqualEventDate() {
        RentalPaymentInputDTO reducedDTO = new RentalPaymentInputDTO(
                1, LocalDate.now().plusDays(10), "PIX", new BigDecimal("100.00"), 1, null, "PENDING"
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any()))
                .thenReturn(new BigDecimal("200.00"))
                .thenReturn(new BigDecimal("100.00"));
        when(paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED)).thenReturn(1L);
        when(paymentRepository.findMaxInstallmentNumberByContractId(contractId)).thenReturn(1);

        LocalDate latestPaymentDate = LocalDate.now().minusDays(10);
        when(paymentRepository.findMaxPaymentDateByContractId(contractId)).thenReturn(Optional.of(latestPaymentDate));

        LocalDate expectedCandidate = latestPaymentDate.plusDays(30);

        RentalPayment gapPayment = RentalPayment.builder()
                .id(UUID.randomUUID())
                .contract(draftContract)
                .installmentNumber(2)
                .paymentDate(expectedCandidate)
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
        assertThat(result.get(1).paymentDate()).isEqualTo(expectedCandidate);
    }
}

