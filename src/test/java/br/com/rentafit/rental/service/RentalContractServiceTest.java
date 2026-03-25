package br.com.rentafit.rental.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.*;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.port.CustomerPort.CustomerSnapshot;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.validation.RentalContractValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
@DisplayName("RentalContractService - Unit Tests")
class RentalContractServiceTest {

    @Mock private RentalContractRepository contractRepository;
    @Mock private RentalContractValidator validator;
    @Mock private RentalWorkflowService workflowService;
    @Mock private RentalMapper mapper;

    @InjectMocks
    private RentalContractService contractService;

    private UUID contractId;
    private UUID customerId;
    private CustomerSnapshot customerSnapshot;
    private RentalContract draftContract;
    private RentalContract signedContract;
    private RentalContractDetailsDTO detailsDTO;
    private RentalContractSummaryDTO summaryDTO;
    private CreateRentalContractDTO createDTO;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        customerSnapshot = new CustomerSnapshot(customerId, "Ana Lima", "12345678901");

        draftContract = RentalContract.builder()
                .id(contractId)
                .customerId(customerId)
                .customerName("Ana Lima")
                .customerDocument("12345678901")
                .status(ContractStatus.DRAFT)
                .pickupDate(LocalDate.now().plusDays(5))
                .eventDate(LocalDate.now().plusDays(7))
                .returnDate(LocalDate.now().plusDays(9))
                .returned(false)
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        signedContract = RentalContract.builder()
                .id(contractId)
                .customerId(customerId)
                .customerName("Ana Lima")
                .customerDocument("12345678901")
                .status(ContractStatus.SIGNED)
                .pickupDate(LocalDate.now().plusDays(5))
                .eventDate(LocalDate.now().plusDays(7))
                .returnDate(LocalDate.now().plusDays(9))
                .returned(false)
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        detailsDTO = RentalContractDetailsDTO.builder()
                .id(contractId)
                .status("DRAFT")
                .statusDescription("Proposta")
                .customerId(customerId)
                .customerName("Ana Lima")
                .totalValue(BigDecimal.ZERO)
                .paidValue(BigDecimal.ZERO)
                .remainingValue(BigDecimal.ZERO)
                .items(List.of())
                .payments(List.of())
                .build();

        summaryDTO = RentalContractSummaryDTO.builder()
                .id(contractId)
                .status("DRAFT")
                .statusDescription("Proposta")
                .build();

        createDTO = new CreateRentalContractDTO(
                customerId, 0, null,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(9),
                "Observação",
                List.of(new ContractItemInputDTO(UUID.randomUUID(), "001", "Vestido de Noiva",
                        new BigDecimal("500.00"), UUID.randomUUID(), List.of())),
                List.of(new RentalPaymentInputDTO(1, LocalDate.now().plusDays(5),
                        "PIX", new BigDecimal("500.00"), 1, null, "PENDING"))
        );
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll deve retornar página de contratos")
    void testFindAll() {
        Page<RentalContract> page = new PageImpl<>(List.of(draftContract));
        when(contractRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);
        when(mapper.toSummaryDTO(draftContract)).thenReturn(summaryDTO);

        Page<RentalContractSummaryDTO> result = contractService.findAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById deve retornar contrato quando existe")
    void testFindById_success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(mapper.toDetailsDTO(draftContract, null)).thenReturn(detailsDTO);

        RentalContractDetailsDTO result = contractService.findById(contractId);

        assertThat(result.id()).isEqualTo(contractId);
    }

    @Test
    @DisplayName("findById deve lançar ResourceNotFoundException quando não encontrado")
    void testFindById_notFound() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.findById(contractId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create deve salvar contrato com snapshot do cliente")
    void testCreate_success() {
        when(validator.validateAndGetCustomer(customerId)).thenReturn(customerSnapshot);
        when(mapper.toEntity(createDTO, customerSnapshot)).thenReturn(draftContract);
        when(contractRepository.saveAndFlush(draftContract)).thenReturn(draftContract);
        when(mapper.toDetailsDTO(draftContract, null)).thenReturn(detailsDTO);

        RentalContractDetailsDTO result = contractService.create(createDTO);

        assertThat(result).isNotNull();
        verify(validator).validateDateOrder(any(), any(), any());
        verify(validator).validateAndGetCustomer(customerId);
        verify(validator).validateItemsHaveAttendant(createDTO.items());
        verify(contractRepository).saveAndFlush(draftContract);
    }

    @Test
    @DisplayName("create deve lançar ValidationException quando item não tiver attendantEmployeeId")
    void testCreate_itemWithoutAttendantId_throwsValidationException() {
        ContractItemInputDTO itemSemAttendant = new ContractItemInputDTO(
                UUID.randomUUID(), "001", "Vestido de Noiva",
                new BigDecimal("500.00"),
                null,
                List.of()
        );
        CreateRentalContractDTO dtoComItemSemAttendant = new CreateRentalContractDTO(
                customerId, 0, null,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(9),
                "Obs",
                List.of(itemSemAttendant),
                List.of(new RentalPaymentInputDTO(1, LocalDate.now().plusDays(5),
                        "PIX", new BigDecimal("500.00"), 1, null, "PENDING"))
        );

        when(validator.validateAndGetCustomer(customerId)).thenReturn(customerSnapshot);
        doThrow(new ValidationException("attendantEmployeeId é obrigatório"))
                .when(validator).validateItemsHaveAttendant(dtoComItemSemAttendant.items());

        assertThatThrownBy(() -> contractService.create(dtoComItemSemAttendant))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("attendantEmployeeId");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update deve lançar ValidationException se status != DRAFT")
    void testUpdate_blockedWhenNotDraft() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(signedContract));

        UpdateRentalContractDTO updateDTO = new UpdateRentalContractDTO(
                0, null,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(9),
                "", List.of(),
                List.of(new RentalPaymentInputDTO(1, LocalDate.now().plusDays(5),
                        "PIX", new BigDecimal("500.00"), 1, null, "PENDING"))
        );

        assertThatThrownBy(() -> contractService.update(contractId, updateDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DRAFT");
    }

    // ── sign ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sign deve mudar status de DRAFT para SIGNED")
    void testSign_success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(validator.checkConflictsForTransition(any(), any(), any())).thenReturn(null);
        when(contractRepository.save(any())).thenReturn(draftContract);
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        contractService.sign(contractId);

        assertThat(draftContract.getStatus()).isEqualTo(ContractStatus.SIGNED);
        verify(validator).checkConflictsForTransition(any(), any(), any());
    }

    @Test
    @DisplayName("sign deve lançar ValidationException se status != DRAFT")
    void testSign_wrongStatus() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(signedContract));

        assertThatThrownBy(() -> contractService.sign(contractId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    @DisplayName("sign deve incluir warnings no DTO quando há conflitos de proximidade")
    void testSign_withWarnings() {
        List<String> warnings = List.of("Item 'Vestido' — ALERTA: evento próximo");
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(validator.checkConflictsForTransition(any(), any(), any())).thenReturn(warnings);
        when(contractRepository.save(any())).thenReturn(draftContract);

        RentalContractDetailsDTO dtoWithWarnings = RentalContractDetailsDTO.builder()
                .id(contractId).status("SIGNED").statusDescription("Assinado")
                .totalValue(BigDecimal.ZERO).paidValue(BigDecimal.ZERO).remainingValue(BigDecimal.ZERO)
                .items(List.of()).payments(List.of()).warnings(warnings).build();
        when(mapper.toDetailsDTO(any(), eq(warnings))).thenReturn(dtoWithWarnings);

        RentalContractDetailsDTO result = contractService.sign(contractId);

        assertThat(result.warnings()).containsExactlyElementsOf(warnings);
    }

    // ── finalize ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("finalize deve mudar status de SIGNED para FINALIZED e acionar workflow")
    void testFinalize_success() {
        signedContract.getItems().add(RentalContractItem.builder()
                .id(UUID.randomUUID()).contract(signedContract)
                .description("Vestido").value(BigDecimal.valueOf(500)).delivered(false)
                .metadata(new ArrayList<>()).build());

        signedContract.getPayments().add(RentalPayment.builder()
                .id(UUID.randomUUID()).contract(signedContract)
                .installmentNumber(1).paymentDate(LocalDate.now().plusDays(5))
                .paymentMethod(PaymentMethod.PIX).value(BigDecimal.valueOf(500))
                .status(PaymentStatus.PAID).installments(1)
                .build());

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(signedContract));
        when(validator.checkConflictsForTransition(any(), any(), any())).thenReturn(null);
        when(contractRepository.save(any())).thenReturn(signedContract);
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        contractService.finalize(contractId);

        assertThat(signedContract.getStatus()).isEqualTo(ContractStatus.FINALIZED);
        verify(workflowService).onFinalize(signedContract);
    }

    @Test
    @DisplayName("finalize deve lançar ValidationException se nenhuma parcela está paga")
    void testFinalize_noPaidPayment() {
        signedContract.getItems().add(RentalContractItem.builder()
                .id(UUID.randomUUID()).contract(signedContract)
                .description("Vestido").value(BigDecimal.valueOf(500)).delivered(false)
                .metadata(new ArrayList<>()).build());

        // Apenas parcela PENDING — nenhuma PAID
        signedContract.getPayments().add(RentalPayment.builder()
                .id(UUID.randomUUID()).contract(signedContract)
                .installmentNumber(1).paymentDate(LocalDate.now().plusDays(5))
                .paymentMethod(PaymentMethod.PIX).value(BigDecimal.valueOf(500))
                .status(PaymentStatus.PENDING).installments(1)
                .build());

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(signedContract));

        assertThatThrownBy(() -> contractService.finalize(contractId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("parcela paga");
    }

    @Test
    @DisplayName("finalize deve lançar ValidationException se não há itens")
    void testFinalize_noItems() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(signedContract));

        assertThatThrownBy(() -> contractService.finalize(contractId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("item");
    }

    // ── processReturn ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("processReturn deve registrar devolução e acionar workflow")
    void testProcessReturn_success() {
        RentalContract finalizedContract = RentalContract.builder()
                .id(contractId).status(ContractStatus.FINALIZED)
                .returned(false).items(new ArrayList<>()).payments(new ArrayList<>())
                .pickupDate(LocalDate.now().minusDays(7))
                .eventDate(LocalDate.now().minusDays(2))
                .returnDate(LocalDate.now())
                .build();

        ReturnContractDTO returnDTO = new ReturnContractDTO(LocalDate.now(), null);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
        when(contractRepository.save(any())).thenReturn(finalizedContract);
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        contractService.processReturn(contractId, returnDTO);

        assertThat(finalizedContract.getReturned()).isTrue();
        verify(workflowService).onReturn(finalizedContract);
    }

    @Test
    @DisplayName("processReturn deve lançar ValidationException se já devolvido")
    void testProcessReturn_alreadyReturned() {
        RentalContract returnedContract = RentalContract.builder()
                .id(contractId).status(ContractStatus.FINALIZED).returned(true)
                .items(new ArrayList<>()).payments(new ArrayList<>()).build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(returnedContract));
        ReturnContractDTO returnDTO = new ReturnContractDTO(LocalDate.now(), null);

        assertThatThrownBy(() -> contractService.processReturn(contractId, returnDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Devolução já processada");
    }

    // ── duplicate ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("duplicate deve criar novo DRAFT com snapshot atualizado e sem pagamentos")
    void testDuplicate_success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(validator.validateAndGetCustomer(customerId)).thenReturn(customerSnapshot);
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        contractService.duplicate(contractId);

        verify(validator).validateAndGetCustomer(customerId);
        verify(contractRepository).save(argThat(c ->
                ContractStatus.DRAFT.equals(c.getStatus())
                && "Ana Lima".equals(c.getCustomerName())
                && "12345678901".equals(c.getCustomerDocument())
                && c.getPayments().isEmpty()
        ));
    }
}

