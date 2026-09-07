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
import org.springframework.test.util.ReflectionTestUtils;
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
import java.time.format.DateTimeFormatter;
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
    private String legacyId;
    private CustomerSnapshot customerSnapshot;
    private RentalContract draftContract;
    private RentalContract signedContract;
    private RentalContractDetailsDTO detailsDTO;
    private RentalContractSummaryDTO summaryDTO;
    private CreateRentalContractDTO createDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contractService, "legacyIdPattern", "yyMMdd");
        contractId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        legacyId = "CTR001";
        customerSnapshot = new CustomerSnapshot(customerId, "Ana Lima", "12345678901");

        draftContract = RentalContract.builder()
                .id(contractId)
                .legacyId(legacyId)
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
                .legacyId(legacyId)
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
                .status(0)
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
                customerId, null, null,
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

    @Test
    @DisplayName("findByLegacyId deve retornar contrato quando existe")
    void testFindByLegacyId_success() {
        when(contractRepository.findByLegacyId(legacyId)).thenReturn(Optional.of(draftContract));
        when(mapper.toDetailsDTO(draftContract, draftContract.getPayments(), null)).thenReturn(detailsDTO);

        RentalContractDetailsDTO result = contractService.findByLegacyId(legacyId, null);

        assertThat(result.id()).isEqualTo(contractId);
        verify(contractRepository).findByLegacyId(legacyId);
    }

    @Test
    @DisplayName("findByLegacyId deve lançar ResourceNotFoundException quando não encontrado")
    void testFindByLegacyId_notFound() {
        when(contractRepository.findByLegacyId(legacyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.findByLegacyId(legacyId, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("legacyId");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create deve salvar contrato com snapshot do cliente e gerar legacyId automaticamente")
    void testCreate_success() {
        RentalContract contractWithoutLegacyId = RentalContract.builder()
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

        when(validator.validateAndGetCustomer(customerId)).thenReturn(customerSnapshot);
        when(mapper.toEntity(createDTO, customerSnapshot)).thenReturn(contractWithoutLegacyId);
        when(contractRepository.findMaxLegacyIdByPrefix(any())).thenReturn(Optional.empty());
        when(contractRepository.saveAndFlush(contractWithoutLegacyId)).thenReturn(contractWithoutLegacyId);
        when(mapper.toDetailsDTO(contractWithoutLegacyId, null)).thenReturn(detailsDTO);

        RentalContractDetailsDTO result = contractService.create(createDTO);

        assertThat(result).isNotNull();
        String todayPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        assertThat(contractWithoutLegacyId.getLegacyId()).isEqualTo(todayPrefix + "1");
        verify(validator).validateDateOrder(any(), any(), any());
        verify(validator).validateAndGetCustomer(customerId);
        verify(validator).validateItemsHaveAttendant(createDTO.items());
        verify(contractRepository).saveAndFlush(contractWithoutLegacyId);
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
                customerId, null, null,
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

    @Test
    @DisplayName("create deve gerar legacyId sequencial quando já existem contratos no dia")
    void testCreate_autoGenerateLegacyId_increment() {
        String todayPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";

        RentalContract contractNoLegacy = RentalContract.builder()
                .id(contractId).customerId(customerId)
                .customerName("Ana Lima").customerDocument("12345678901")
                .status(ContractStatus.DRAFT)
                .pickupDate(LocalDate.now().plusDays(5))
                .eventDate(LocalDate.now().plusDays(7))
                .returnDate(LocalDate.now().plusDays(9))
                .returned(false).items(new ArrayList<>()).payments(new ArrayList<>())
                .build();

        when(validator.validateAndGetCustomer(customerId)).thenReturn(customerSnapshot);
        when(mapper.toEntity(createDTO, customerSnapshot)).thenReturn(contractNoLegacy);
        when(contractRepository.findMaxLegacyIdByPrefix(todayPrefix))
                .thenReturn(Optional.of(todayPrefix + "3"));
        when(contractRepository.saveAndFlush(contractNoLegacy)).thenReturn(contractNoLegacy);
        when(mapper.toDetailsDTO(contractNoLegacy, null)).thenReturn(detailsDTO);

        contractService.create(createDTO);

        assertThat(contractNoLegacy.getLegacyId()).isEqualTo(todayPrefix + "4");
    }

    @Test
    @DisplayName("create deve preservar legacyId fornecido no DTO (importação legado)")
    void testCreate_preserveProvidedLegacyId() {
        CreateRentalContractDTO dtoWithLegacy = new CreateRentalContractDTO(
                customerId, null, "12345",
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(9),
                "Obs",
                List.of(new ContractItemInputDTO(UUID.randomUUID(), "001", "Vestido",
                        new BigDecimal("500.00"), UUID.randomUUID(), List.of())),
                List.of(new RentalPaymentInputDTO(1, LocalDate.now().plusDays(5),
                        "PIX", new BigDecimal("500.00"), 1, null, "PENDING"))
        );

        RentalContract contractWithLegacy = RentalContract.builder()
                .id(contractId).legacyId("12345").customerId(customerId)
                .customerName("Ana Lima").customerDocument("12345678901")
                .status(ContractStatus.DRAFT)
                .pickupDate(LocalDate.now().plusDays(5))
                .eventDate(LocalDate.now().plusDays(7))
                .returnDate(LocalDate.now().plusDays(9))
                .returned(false).items(new ArrayList<>()).payments(new ArrayList<>())
                .build();

        when(validator.validateAndGetCustomer(customerId)).thenReturn(customerSnapshot);
        when(mapper.toEntity(dtoWithLegacy, customerSnapshot)).thenReturn(contractWithLegacy);
        when(contractRepository.saveAndFlush(contractWithLegacy)).thenReturn(contractWithLegacy);
        when(mapper.toDetailsDTO(contractWithLegacy, null)).thenReturn(detailsDTO);

        contractService.create(dtoWithLegacy);

        assertThat(contractWithLegacy.getLegacyId()).isEqualTo("12345");
        verify(contractRepository, never()).findMaxLegacyIdByPrefix(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update deve lançar ValidationException se status != DRAFT e != REVISION")
    void testUpdate_blockedWhenNotDraftOrRevision() {
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

    @Test
    @DisplayName("update deve criar parcela PENDING automática quando parcelas somam menos que itens")
    void testUpdate_autoCreatesDeficitPayment() {
        LocalDate eventDate = LocalDate.now().plusDays(60);

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        // Retorna déficit de 200 (itens=500, parcelas=300)
        when(validator.calculatePaymentDeficit(any(), any())).thenReturn(new BigDecimal("200.00"));
        when(contractRepository.save(any())).thenReturn(draftContract);
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        UpdateRentalContractDTO updateDTO = new UpdateRentalContractDTO(
                0, null,
                LocalDate.now().plusDays(5),
                eventDate,
                eventDate.plusDays(2),
                "Obs",
                List.of(new ContractItemInputDTO(UUID.randomUUID(), "001", "Vestido",
                        new BigDecimal("500.00"), UUID.randomUUID(), List.of())),
                List.of(new RentalPaymentInputDTO(1, LocalDate.now().plusDays(5),
                        "PIX", new BigDecimal("300.00"), 1, null, "PENDING"))
        );

        contractService.update(contractId, updateDTO);

        // Verifica que o mapper recebeu a lista com a parcela extra
        verify(mapper).updateEntityFromDTO(any(), eq(updateDTO), argThat(payments ->
                payments.size() == 2
                && payments.get(1).installmentNumber() == 2
                && payments.get(1).value().compareTo(new BigDecimal("200.00")) == 0
                && "PIX".equals(payments.get(1).paymentMethod())
                && "PENDING".equals(payments.get(1).status())
        ));
    }

    @Test
    @DisplayName("update deve usar eventDate quando última parcela + 30 dias ultrapassa eventDate")
    void testUpdate_autoPaymentDateCappedAtEventDate() {
        LocalDate eventDate = LocalDate.now().plusDays(10);

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(validator.calculatePaymentDeficit(any(), any())).thenReturn(new BigDecimal("100.00"));
        when(contractRepository.save(any())).thenReturn(draftContract);
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        // Parcela existente com data = hoje+5, eventDate = hoje+10
        // hoje+5 + 30 = hoje+35 > eventDate → deve usar eventDate
        UpdateRentalContractDTO updateDTO = new UpdateRentalContractDTO(
                0, null,
                LocalDate.now().plusDays(3),
                eventDate,
                eventDate.plusDays(2),
                "Obs",
                List.of(new ContractItemInputDTO(UUID.randomUUID(), "001", "Vestido",
                        new BigDecimal("500.00"), UUID.randomUUID(), List.of())),
                List.of(new RentalPaymentInputDTO(1, LocalDate.now().plusDays(5),
                        "PIX", new BigDecimal("400.00"), 1, null, "PENDING"))
        );

        contractService.update(contractId, updateDTO);

        verify(mapper).updateEntityFromDTO(any(), eq(updateDTO), argThat(payments ->
                payments.size() == 2
                && payments.get(1).paymentDate().equals(eventDate) // capped at eventDate
        ));
    }

    @Test
    @DisplayName("update deve usar última data + 30 quando resultado cabe antes do eventDate")
    void testUpdate_autoPaymentDatePlus30Days() {
        LocalDate eventDate = LocalDate.now().plusDays(60);
        LocalDate lastPaymentDate = LocalDate.now().plusDays(5);

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(validator.calculatePaymentDeficit(any(), any())).thenReturn(new BigDecimal("100.00"));
        when(contractRepository.save(any())).thenReturn(draftContract);
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        UpdateRentalContractDTO updateDTO = new UpdateRentalContractDTO(
                0, null,
                LocalDate.now().plusDays(3),
                eventDate,
                eventDate.plusDays(2),
                "Obs",
                List.of(new ContractItemInputDTO(UUID.randomUUID(), "001", "Vestido",
                        new BigDecimal("500.00"), UUID.randomUUID(), List.of())),
                List.of(new RentalPaymentInputDTO(1, lastPaymentDate,
                        "PIX", new BigDecimal("400.00"), 1, null, "PENDING"))
        );

        contractService.update(contractId, updateDTO);

        verify(mapper).updateEntityFromDTO(any(), eq(updateDTO), argThat(payments ->
                payments.size() == 2
                && payments.get(1).paymentDate().equals(lastPaymentDate.plusDays(30))
        ));
    }

    @Test
    @DisplayName("update não deve criar parcela automática quando parcelas batem com total")
    void testUpdate_noAutoPaymentWhenExact() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(validator.calculatePaymentDeficit(any(), any())).thenReturn(BigDecimal.ZERO);
        when(contractRepository.save(any())).thenReturn(draftContract);
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        UpdateRentalContractDTO updateDTO = new UpdateRentalContractDTO(
                0, null,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(9),
                "Obs",
                List.of(new ContractItemInputDTO(UUID.randomUUID(), "001", "Vestido",
                        new BigDecimal("500.00"), UUID.randomUUID(), List.of())),
                List.of(new RentalPaymentInputDTO(1, LocalDate.now().plusDays(5),
                        "PIX", new BigDecimal("500.00"), 1, null, "PENDING"))
        );

        contractService.update(contractId, updateDTO);

        // Verifica que o mapper recebeu exatamente 1 parcela (a original)
        verify(mapper).updateEntityFromDTO(any(), eq(updateDTO), argThat(payments ->
                payments.size() == 1
        ));
    }

    @Test
    @DisplayName("update deve lançar ValidationException se parcelas excedem o total dos itens")
    void testUpdate_rejectsOverpayment() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        doThrow(new ValidationException("A soma das parcelas (600.00) ultrapassa o valor total do contrato (500.00)"))
                .when(validator).validatePaymentsNotExceedTotal(any(), any());

        UpdateRentalContractDTO updateDTO = new UpdateRentalContractDTO(
                0, null,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(9),
                "Obs",
                List.of(new ContractItemInputDTO(UUID.randomUUID(), "001", "Vestido",
                        new BigDecimal("500.00"), UUID.randomUUID(), List.of())),
                List.of(new RentalPaymentInputDTO(1, LocalDate.now().plusDays(5),
                        "PIX", new BigDecimal("600.00"), 1, null, "PENDING"))
        );

        assertThatThrownBy(() -> contractService.update(contractId, updateDTO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ultrapassa");
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
    @DisplayName("sign deve lançar ValidationException se status != DRAFT e != REVISION")
    void testSign_wrongStatus() {
        RentalContract finalizedContract = RentalContract.builder()
                .id(contractId).status(ContractStatus.FINALIZED)
                .returned(false).items(new ArrayList<>()).payments(new ArrayList<>())
                .pickupDate(LocalDate.now().plusDays(5))
                .eventDate(LocalDate.now().plusDays(7))
                .returnDate(LocalDate.now().plusDays(9))
                .build();
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

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
                .id(contractId).status(1).statusDescription("Assinado")
                .totalValue(BigDecimal.ZERO).paidValue(BigDecimal.ZERO).remainingValue(BigDecimal.ZERO)
                .items(List.of()).payments(List.of()).warnings(warnings).build();
        when(mapper.toDetailsDTO(any(), eq(warnings))).thenReturn(dtoWithWarnings);

        RentalContractDetailsDTO result = contractService.sign(contractId);

        assertThat(result.warnings()).containsExactlyElementsOf(warnings);
    }

    @Test
    @DisplayName("sign deve lançar ValidationException quando há conflito BLOCKING")
    void testSign_blockingConflict() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(validator.checkConflictsForTransition(any(), any(), any()))
                .thenThrow(new ValidationException("Conflito de reserva: Item 'Vestido' — BLOQUEIO — mesma data"));

        assertThatThrownBy(() -> contractService.sign(contractId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("BLOQUEIO");

        // Status NÃO deve mudar
        assertThat(draftContract.getStatus()).isEqualTo(ContractStatus.DRAFT);
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

    @Test
    @DisplayName("finalize deve incluir warnings no DTO quando há conflitos de proximidade")
    void testFinalize_withWarnings() {
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

        List<String> warnings = List.of("Item 'Vestido' — ALERTA: evento próximo");
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(signedContract));
        when(validator.checkConflictsForTransition(any(), any(), any())).thenReturn(warnings);
        when(contractRepository.save(any())).thenReturn(signedContract);

        RentalContractDetailsDTO dtoWithWarnings = RentalContractDetailsDTO.builder()
                .id(contractId).status(2).statusDescription("Fechado")
                .totalValue(BigDecimal.valueOf(500)).paidValue(BigDecimal.valueOf(500))
                .remainingValue(BigDecimal.ZERO)
                .items(List.of()).payments(List.of()).warnings(warnings).build();
        when(mapper.toDetailsDTO(any(), eq(warnings))).thenReturn(dtoWithWarnings);

        RentalContractDetailsDTO result = contractService.finalize(contractId);

        assertThat(result.warnings()).containsExactlyElementsOf(warnings);
        verify(workflowService).onFinalize(signedContract);
    }

    @Test
    @DisplayName("finalize deve lançar ValidationException quando há conflito BLOCKING")
    void testFinalize_blockingConflict() {
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
        when(validator.checkConflictsForTransition(any(), any(), any()))
                .thenThrow(new ValidationException("Conflito de reserva: Item 'Vestido' — BLOQUEIO — mesma data"));

        assertThatThrownBy(() -> contractService.finalize(contractId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("BLOQUEIO");

        assertThat(signedContract.getStatus()).isEqualTo(ContractStatus.SIGNED);
        verify(workflowService, never()).onFinalize(any());
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
    @DisplayName("duplicate deve criar novo DRAFT com snapshot atualizado, sem pagamentos e legacyId gerado")
    void testDuplicate_success() {
        String todayPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));
        when(validator.validateAndGetCustomer(customerId)).thenReturn(customerSnapshot);
        when(contractRepository.findMaxLegacyIdByPrefix(todayPrefix))
                .thenReturn(Optional.of(todayPrefix + "5"));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        contractService.duplicate(contractId);

        verify(validator).validateAndGetCustomer(customerId);
        verify(contractRepository).save(argThat(c ->
                ContractStatus.DRAFT.equals(c.getStatus())
                && "Ana Lima".equals(c.getCustomerName())
                && "12345678901".equals(c.getCustomerDocument())
                && c.getPayments().isEmpty()
                && (todayPrefix + "6").equals(c.getLegacyId())
        ));
    }

    // ── revise ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("revise deve criar REVISION com itens e pagamentos copiados")
    void testRevise_success() {
        String todayPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";

        // Adiciona item e pagamento ao contrato assinado
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
        when(contractRepository.findByParentContractIdAndStatusNot(contractId, ContractStatus.SUPERSEDED))
                .thenReturn(Optional.empty());
        when(validator.validateAndGetCustomer(customerId)).thenReturn(customerSnapshot);
        when(contractRepository.findMaxLegacyIdByPrefix(todayPrefix)).thenReturn(Optional.empty());
        when(contractRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        contractService.revise(contractId);

        verify(contractRepository).saveAndFlush(argThat(c ->
                ContractStatus.REVISION.equals(c.getStatus())
                && contractId.equals(c.getParentContractId())
                && c.getItems().size() == 1
                && c.getPayments().size() == 1
                && PaymentStatus.PAID.equals(c.getPayments().get(0).getStatus())
                && (todayPrefix + "1").equals(c.getLegacyId())
        ));
    }

    @Test
    @DisplayName("revise deve lançar ValidationException se contrato não está SIGNED")
    void testRevise_wrongStatus() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(draftContract));

        assertThatThrownBy(() -> contractService.revise(contractId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("SIGNED");
    }

    @Test
    @DisplayName("revise deve retornar revisão existente se já houver uma ativa")
    void testRevise_returnsExistingRevision() {
        UUID existingRevisionId = UUID.randomUUID();
        RentalContract existingRevision = RentalContract.builder()
                .id(existingRevisionId).status(ContractStatus.REVISION)
                .parentContractId(contractId)
                .items(new ArrayList<>()).payments(new ArrayList<>())
                .build();

        RentalContractDetailsDTO existingRevisionDTO = RentalContractDetailsDTO.builder()
                .id(existingRevisionId).status(3).statusDescription("Revisão")
                .parentContractId(contractId)
                .totalValue(BigDecimal.ZERO).paidValue(BigDecimal.ZERO).remainingValue(BigDecimal.ZERO)
                .items(List.of()).payments(List.of()).build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(signedContract));
        when(contractRepository.findByParentContractIdAndStatusNot(contractId, ContractStatus.SUPERSEDED))
                .thenReturn(Optional.of(existingRevision));
        when(mapper.toDetailsDTO(existingRevision, null)).thenReturn(existingRevisionDTO);

        RentalContractDetailsDTO result = contractService.revise(contractId);

        assertThat(result.id()).isEqualTo(existingRevisionId);
        assertThat(result.parentContractId()).isEqualTo(contractId);
        verify(contractRepository, never()).saveAndFlush(any());
    }

    // ── sign (REVISION → SIGNED + parent SUPERSEDED) ─────────────────────────

    @Test
    @DisplayName("sign de REVISION deve assinar e marcar contrato-pai como SUPERSEDED")
    void testSign_revision_supersedesParent() {
        UUID parentId = UUID.randomUUID();

        RentalContract revisionContract = RentalContract.builder()
                .id(contractId)
                .status(ContractStatus.REVISION)
                .parentContractId(parentId)
                .customerId(customerId)
                .pickupDate(LocalDate.now().plusDays(5))
                .eventDate(LocalDate.now().plusDays(7))
                .returnDate(LocalDate.now().plusDays(9))
                .returned(false)
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        RentalContract parentContract = RentalContract.builder()
                .id(parentId)
                .status(ContractStatus.SIGNED)
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(revisionContract));
        when(contractRepository.findById(parentId)).thenReturn(Optional.of(parentContract));
        when(validator.checkConflictsForTransition(any(), any(), any())).thenReturn(null);
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        contractService.sign(contractId);

        assertThat(revisionContract.getStatus()).isEqualTo(ContractStatus.SIGNED);
        assertThat(parentContract.getStatus()).isEqualTo(ContractStatus.SUPERSEDED);
        assertThat(parentContract.getReplacedByContractId()).isEqualTo(contractId);
        verify(contractRepository, times(2)).save(any());
    }

    // ── findByCustomer ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByCustomer deve retornar página de contratos do cliente")
    void testFindByCustomer_success() {
        Page<RentalContract> page = new PageImpl<>(List.of(draftContract));
        when(contractRepository.findByCustomerId(customerId, PageRequest.of(0, 10))).thenReturn(page);
        when(mapper.toSummaryDTO(draftContract)).thenReturn(summaryDTO);

        Page<RentalContractSummaryDTO> result = contractService.findByCustomer(customerId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(contractRepository).findByCustomerId(customerId, PageRequest.of(0, 10));
    }

    // ── deliverItem ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deliverItem deve confirmar entrega de item em contrato FINALIZED")
    void testDeliverItem_success() {
        UUID itemId = UUID.randomUUID();
        UUID attendantId = UUID.randomUUID();
        RentalContractItem item = RentalContractItem.builder()
                .id(itemId).contract(signedContract)
                .description("Vestido").value(BigDecimal.valueOf(500)).delivered(false)
                .metadata(new ArrayList<>()).build();

        RentalContract finalizedContract = RentalContract.builder()
                .id(contractId).status(ContractStatus.FINALIZED)
                .returned(false).items(new ArrayList<>(List.of(item))).payments(new ArrayList<>())
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
        when(mapper.toDetailsDTO(any(), isNull())).thenReturn(detailsDTO);

        contractService.deliverItem(contractId, itemId, attendantId);

        verify(workflowService).onDeliverItem(item, attendantId);
    }

    @Test
    @DisplayName("deliverItem deve lançar ValidationException se contrato não está FINALIZED")
    void testDeliverItem_wrongStatus() {
        UUID itemId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(signedContract));

        assertThatThrownBy(() -> contractService.deliverItem(contractId, itemId, UUID.randomUUID()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("FINALIZED");
    }

    @Test
    @DisplayName("deliverItem deve lançar ResourceNotFoundException se item não existe no contrato")
    void testDeliverItem_itemNotFound() {
        RentalContract finalizedContract = RentalContract.builder()
                .id(contractId).status(ContractStatus.FINALIZED)
                .returned(false).items(new ArrayList<>()).payments(new ArrayList<>()).build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

        assertThatThrownBy(() -> contractService.deliverItem(contractId, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RentalContractItem");
    }

    @Test
    @DisplayName("deliverItem deve lançar ValidationException se item já foi entregue")
    void testDeliverItem_alreadyDelivered() {
        UUID itemId = UUID.randomUUID();
        RentalContractItem item = RentalContractItem.builder()
                .id(itemId).delivered(true).metadata(new ArrayList<>()).build();

        RentalContract finalizedContract = RentalContract.builder()
                .id(contractId).status(ContractStatus.FINALIZED)
                .returned(false).items(new ArrayList<>(List.of(item))).payments(new ArrayList<>()).build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

        assertThatThrownBy(() -> contractService.deliverItem(contractId, itemId, UUID.randomUUID()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("já marcado como entregue");
    }
}
