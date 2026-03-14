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
    @DisplayName("updatePayment deve lançar ValidationException em contrato FINALIZED")
    void testUpdatePayment_blockedWhenFinalized() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

        assertThatThrownBy(() -> paymentService.updatePayment(contractId, paymentId, validPaymentDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("FINALIZADO");
    }

    @Test
    @DisplayName("updatePayment deve atualizar parcela em contrato DRAFT")
    void testUpdatePayment_success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(paymentRepository.findByIdAndContractId(paymentId, contractId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.sumValueByContractIdAndStatusIn(eq(contractId), any())).thenReturn(new BigDecimal("200.00"));
        when(paymentRepository.save(existingPayment)).thenReturn(existingPayment);
        when(mapper.toPaymentDetailsDTO(existingPayment)).thenReturn(detailsDTO);

        RentalPaymentDetailsDTO result = paymentService.updatePayment(contractId, paymentId, validPaymentDTO);

        assertThat(result).isNotNull();
        verify(paymentRepository).save(existingPayment);
    }

    // ── cancelPayment ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelPayment deve lançar ValidationException em contrato FINALIZED")
    void testCancelPayment_blockedWhenFinalized() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

        assertThatThrownBy(() -> paymentService.cancelPayment(contractId, paymentId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("FINALIZADO");
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
}

