package br.com.rentafit.rental.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.*;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.port.CustomerPort.CustomerSnapshot;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.validation.RentalContractValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orquestrador principal do ciclo de vida dos contratos de locação.
 *
 * <p>Fluxo de transições:
 * DRAFT → (sign) → SIGNED → (finalize) → FINALIZED → (processReturn) → returned=true
 * </p>
 *
 * <p>Invariantes:
 * <ul>
 *   <li>update() bloqueado se status != DRAFT.</li>
 *   <li>Snapshot do cliente gravado na criação e nunca alterado.</li>
 *   <li>Conflitos de reserva verificados apenas em sign() e finalize().</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RentalContractService {

    private final RentalContractRepository contractRepository;
    private final RentalContractValidator validator;
    private final RentalWorkflowService workflowService;
    private final RentalMapper mapper;

    private static final DateTimeFormatter LEGACY_ID_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<RentalContractSummaryDTO> findAll(Pageable pageable) {
        return contractRepository.findAll(pageable).map(mapper::toSummaryDTO);
    }

    @Transactional(readOnly = true)
    public RentalContractDetailsDTO findById(UUID id) {
        RentalContract contract = requireContract(id);
        return mapper.toDetailsDTO(contract, null);
    }

    @Transactional(readOnly = true)
    public RentalContractDetailsDTO findByLegacyId(String leg, Integer installmentNumber) {
        RentalContract contract = requireContractByLegacyId(leg);
        List<RentalPayment> payments = installmentNumber == null
                ? contract.getPayments()
                : contract.getPayments().stream()
                        .filter(p -> installmentNumber.equals(p.getInstallmentNumber()))
                        .collect(Collectors.toList());
        return mapper.toDetailsDTO(contract, payments, null);
    }

    @Transactional(readOnly = true)
    public Page<RentalContractSummaryDTO> findByCustomer(UUID customerId, Pageable pageable) {
        return contractRepository.findByCustomerId(customerId, pageable).map(mapper::toSummaryDTO);
    }

    public RentalContractDetailsDTO create(CreateRentalContractDTO dto) {
        // Validação básica de datas
        validator.validateDateOrder(dto.pickupDate(), dto.eventDate(), dto.returnDate());

        // Valida e obtém snapshot do cliente
        CustomerSnapshot snapshot = validator.validateAndGetCustomer(dto.customerId());


        validator.validateItemsHaveAttendant(dto.items());

        // Valida disponibilidade dos itens e acessórios
        List<UUID> rentalItemIds = dto.items().stream()
                .map(ContractItemInputDTO::rentalItemId).collect(Collectors.toList());
        validator.validateItemsAvailability(rentalItemIds);

        List<UUID> accessoryIds = dto.items().stream()
                .filter(i -> i.metadata() != null)
                .flatMap(i -> i.metadata().stream())
                .map(ContractItemMetaInputDTO::accessoryId)
                .collect(Collectors.toList());
        validator.validateAccessoriesAvailability(accessoryIds);

        // Valida que parcelas PAID possuem funcionário responsável
        validator.validatePaidPaymentsHaveEmployee(dto.payments());

        // Valida que as parcelas somam o valor total dos itens
        validator.validatePaymentsMatchTotal(dto.payments(), dto.items());

        RentalContract contract = mapper.toEntity(dto, snapshot);

        // Auto-generate legacyId when not provided in the request
        if (contract.getLegacyId() == null || contract.getLegacyId().isBlank()) {
            contract.setLegacyId(generateLegacyId());
        }

        // saveAndFlush forces an immediate INSERT so @CreationTimestamp is populated
        // by Hibernate before the mapper reads the createdAt field
        RentalContract saved = contractRepository.saveAndFlush(contract);
        log.info("Created rental contract {} (legacyId={}) for customer {}",
                saved.getId(), saved.getLegacyId(), snapshot.id());
        return mapper.toDetailsDTO(saved, null);
    }

    public RentalContractDetailsDTO update(UUID id, UpdateRentalContractDTO dto) {
        RentalContract contract = requireContract(id);

        if (!ContractStatus.DRAFT.equals(contract.getStatus())
                && !ContractStatus.REVISION.equals(contract.getStatus())) {
            throw new ValidationException(
                    "Apenas contratos em DRAFT ou REVISION podem ser atualizados. Status atual: " + contract.getStatus());
        }

        validator.validateDateOrder(dto.pickupDate(), dto.eventDate(), dto.returnDate());

        validator.validateItemsHaveAttendant(dto.items());

        List<UUID> rentalItemIds = dto.items().stream()
                .map(ContractItemInputDTO::rentalItemId).collect(Collectors.toList());
        validator.validateItemsAvailability(rentalItemIds);

        List<UUID> accessoryIds = dto.items().stream()
                .filter(i -> i.metadata() != null)
                .flatMap(i -> i.metadata().stream())
                .map(ContractItemMetaInputDTO::accessoryId)
                .collect(Collectors.toList());
        validator.validateAccessoriesAvailability(accessoryIds);

        // Valida que parcelas PAID possuem funcionário responsável
        validator.validatePaidPaymentsHaveEmployee(dto.payments());

        // Em REVISION, parcelas PAID do contrato original não podem ser alteradas/removidas
        if (ContractStatus.REVISION.equals(contract.getStatus())) {
            validator.validateRevisionPaymentIntegrity(dto.payments(), contract.getPayments());
        }

        // Valida que parcelas não ultrapassam o total dos itens (excesso → erro)
        validator.validatePaymentsNotExceedTotal(dto.payments(), dto.items());

        // Se a soma das parcelas é menor que o total, cria parcela PENDING automática com o restante
        List<RentalPaymentInputDTO> payments = autoCompletePayments(dto.payments(), dto.items(), dto.eventDate());

        mapper.updateEntityFromDTO(contract, dto, payments);
        RentalContract saved = contractRepository.save(contract);
        log.info("Updated rental contract {}", id);
        return mapper.toDetailsDTO(saved, null);
    }

    // ── Transições de Estado ──────────────────────────────────────────────────

    /**
     * DRAFT|REVISION → SIGNED.
     * Executa checagem de conflitos. BLOCKING → 422; WARNING → retornado no DTO.
     * Se o contrato for uma REVISION (parentContractId != null),
     * o contrato-pai é automaticamente marcado como SUPERSEDED.
     */
    public RentalContractDetailsDTO sign(UUID id) {
        RentalContract contract = requireContract(id);
        requireStatusOneOf(contract, "assinar", ContractStatus.DRAFT, ContractStatus.REVISION);

        validator.validateDateOrder(contract.getPickupDate(), contract.getEventDate(), contract.getReturnDate());
        validator.validatePersistedPaidPaymentsHaveEmployee(contract.getPayments());

        List<String> warnings = validator.checkConflictsForTransition(
                contract.getItems(), contract.getEventDate(), contract.getId());

        contract.setStatus(ContractStatus.SIGNED);
        RentalContract saved = contractRepository.save(contract);

        // Supersede the parent contract if this is a revision
        if (saved.getParentContractId() != null) {
            RentalContract parent = requireContract(saved.getParentContractId());
            parent.setStatus(ContractStatus.SUPERSEDED);
            parent.setReplacedByContractId(saved.getId());
            contractRepository.save(parent);
            log.info("Parent contract {} superseded by revision {}", parent.getId(), saved.getId());
        }

        log.info("Contract {} signed. Warnings: {}", id, warnings != null ? warnings.size() : 0);
        return mapper.toDetailsDTO(saved, warnings);
    }

    /**
     * SIGNED → FINALIZED.
     * Re-valida conflitos e aciona o workflow de reserva.
     */
    public RentalContractDetailsDTO finalize(UUID id) {
        RentalContract contract = requireContract(id);
        requireStatus(contract, ContractStatus.SIGNED, "finalizar");

        if (contract.getItems().isEmpty()) {
            throw new ValidationException("O contrato deve ter ao menos um item para ser finalizado");
        }

        boolean hasPaidPayment = contract.getPayments().stream()
                .anyMatch(p -> PaymentStatus.PAID.equals(p.getStatus()));
        if (!hasPaidPayment) {
            throw new ValidationException("O contrato deve ter ao menos uma parcela paga para ser finalizado");
        }

        List<String> warnings = validator.checkConflictsForTransition(
                contract.getItems(), contract.getEventDate(), contract.getId());

        contract.setStatus(ContractStatus.FINALIZED);
        RentalContract saved = contractRepository.save(contract);

        workflowService.onFinalize(saved);

        log.info("Contract {} finalized. Warnings: {}", id, warnings != null ? warnings.size() : 0);
        return mapper.toDetailsDTO(saved, warnings);
    }

    /**
     * Confirma a entrega de um item específico do contrato.
     * Disponível somente para contratos FINALIZED.
     */
    public RentalContractDetailsDTO deliverItem(UUID contractId, UUID itemId, UUID attendantEmployeeId) {
        RentalContract contract = requireContract(contractId);
        requireStatus(contract, ContractStatus.FINALIZED, "confirmar entrega");

        RentalContractItem item = contract.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("RentalContractItem", "id", itemId.toString()));

        if (Boolean.TRUE.equals(item.getDelivered())) {
            throw new ValidationException("Item já marcado como entregue");
        }

        workflowService.onDeliverItem(item, attendantEmployeeId);
        log.info("Item {} delivered in contract {}", itemId, contractId);
        return mapper.toDetailsDTO(contractRepository.findById(contractId).orElseThrow(), null);
    }

    /**
     * Processa a devolução do contrato.
     * Marca como returned e aciona workflow de manutenção.
     */
    public RentalContractDetailsDTO processReturn(UUID id, ReturnContractDTO dto) {
        RentalContract contract = requireContract(id);
        requireStatus(contract, ContractStatus.FINALIZED, "processar devolução");

        if (Boolean.TRUE.equals(contract.getReturned())) {
            throw new ValidationException("Devolução já processada para este contrato");
        }

        contract.setActualReturnDate(dto.actualReturnDate());
        contract.setReturnedByEmployeeId(dto.returnedByEmployeeId());
        contract.setReturned(true);
        RentalContract saved = contractRepository.save(contract);

        workflowService.onReturn(saved);
        log.info("Contract {} return processed", id);
        return mapper.toDetailsDTO(saved, null);
    }

    // ── Revisão ───────────────────────────────────────────────────────────────

    /**
     * Cria uma revisão de um contrato SIGNED.
     * <p>Copia itens e pagamentos; o novo contrato nasce em REVISION.
     * Ao ser assinado, o contrato-pai será marcado como SUPERSEDED.</p>
     */
    public RentalContractDetailsDTO revise(UUID id) {
        RentalContract original = requireContract(id);
        requireStatus(original, ContractStatus.SIGNED, "criar revisão de");

        // Se já existe uma revisão ativa para este contrato, retorna ela
        Optional<RentalContract> activeRevision =
                contractRepository.findByParentContractIdAndStatusNot(id, ContractStatus.SUPERSEDED);
        if (activeRevision.isPresent()) {
            log.info("Active revision {} already exists for contract {}", activeRevision.get().getId(), id);
            return mapper.toDetailsDTO(activeRevision.get(), null);
        }

        CustomerSnapshot freshSnapshot = validator.validateAndGetCustomer(original.getCustomerId());

        RentalContract revision = RentalContract.builder()
                .contractType(original.getContractType())
                .customerId(freshSnapshot.id())
                .customerName(freshSnapshot.name())
                .customerDocument(freshSnapshot.document())
                .createdByEmployeeId(original.getCreatedByEmployeeId())
                .pickupDate(original.getPickupDate())
                .eventDate(original.getEventDate())
                .returnDate(original.getReturnDate())
                .notes("Revisão do contrato " + original.getLegacyId() + ". " + original.getNotes())
                .status(ContractStatus.REVISION)
                .returned(false)
                .parentContractId(original.getId())
                .build();

        // Deep-copy items + metadata
        List<RentalContractItem> copiedItems = original.getItems().stream().map(origItem -> {
            RentalContractItem newItem = RentalContractItem.builder()
                    .contract(revision)
                    .rentalItemId(origItem.getRentalItemId())
                    .legacyProductCode(origItem.getLegacyProductCode())
                    .description(origItem.getDescription())
                    .value(origItem.getValue())
                    .attendantEmployeeId(origItem.getAttendantEmployeeId())
                    .delivered(false)
                    .build();

            List<RentalContractItemMeta> copiedMeta = origItem.getMetadata().stream().map(origMeta ->
                    RentalContractItemMeta.builder()
                            .contractItem(newItem)
                            .type(origMeta.getType())
                            .description(origMeta.getDescription())
                            .accessoryId(origMeta.getAccessoryId())
                            .build()
            ).collect(Collectors.toList());
            newItem.setMetadata(copiedMeta);
            return newItem;
        }).collect(Collectors.toList());
        revision.setItems(copiedItems);

        // Deep-copy payments (preserving status — PAID payments stay PAID)
        List<RentalPayment> copiedPayments = original.getPayments().stream().map(origPay ->
                RentalPayment.builder()
                        .contract(revision)
                        .installmentNumber(origPay.getInstallmentNumber())
                        .paymentDate(origPay.getPaymentDate())
                        .paymentMethod(origPay.getPaymentMethod())
                        .value(origPay.getValue())
                        .installments(origPay.getInstallments())
                        .processedByEmployeeId(origPay.getProcessedByEmployeeId())
                        .status(origPay.getStatus())
                        .build()
        ).collect(Collectors.toList());
        revision.setPayments(copiedPayments);

        revision.setLegacyId(generateLegacyId());

        RentalContract saved = contractRepository.saveAndFlush(revision);
        log.info("Contract {} revised as {} (legacyId={})", id, saved.getId(), saved.getLegacyId());
        original.setReplacedByContractId(saved.getId());
        contractRepository.saveAndFlush(original);
        log.info("Contract Original {} child replaced by {}", original.getId(), saved.getId());

        return mapper.toDetailsDTO(saved, null);
    }

    /**
     * Duplica um contrato como novo DRAFT.
     * Snapshot do cliente é atualizado; itens são copiados; pagamentos são zerados.
     */
    public RentalContractDetailsDTO duplicate(UUID id) {
        RentalContract original = requireContract(id);

        // Atualiza snapshot do cliente
        CustomerSnapshot freshSnapshot = validator.validateAndGetCustomer(original.getCustomerId());

        RentalContract duplicate = RentalContract.builder()
                .contractType(original.getContractType())
                .customerId(freshSnapshot.id())
                .customerName(freshSnapshot.name())
                .customerDocument(freshSnapshot.document())
                .createdByEmployeeId(original.getCreatedByEmployeeId())
                .pickupDate(original.getPickupDate())
                .eventDate(original.getEventDate())
                .returnDate(original.getReturnDate())
                .notes("Duplicado do contrato " + original.getId() + ". " + original.getNotes())
                .status(ContractStatus.DRAFT)
                .returned(false)
                .build();

        List<RentalContractItem> copiedItems = original.getItems().stream().map(origItem -> {
            RentalContractItem newItem = RentalContractItem.builder()
                    .contract(duplicate)
                    .rentalItemId(origItem.getRentalItemId())
                    .legacyProductCode(origItem.getLegacyProductCode())
                    .description(origItem.getDescription())
                    .value(origItem.getValue())
                    .delivered(false)
                    .build();

            List<RentalContractItemMeta> copiedMeta = origItem.getMetadata().stream().map(origMeta ->
                    RentalContractItemMeta.builder()
                            .contractItem(newItem)
                            .type(origMeta.getType())
                            .description(origMeta.getDescription())
                            .accessoryId(origMeta.getAccessoryId())
                            .build()
            ).collect(Collectors.toList());
            newItem.setMetadata(copiedMeta);
            return newItem;
        }).collect(Collectors.toList());

        duplicate.setItems(copiedItems);
        duplicate.setLegacyId(generateLegacyId());

        RentalContract saved = contractRepository.save(duplicate);
        log.info("Contract {} duplicated as {} (legacyId={})", id, saved.getId(), saved.getLegacyId());
        return mapper.toDetailsDTO(saved, null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Se a soma das parcelas é menor que o total dos itens, cria uma parcela PENDING
     * automática com o valor restante e a próxima data compatível.
     *
     * <p>Regras de data da nova parcela:
     * <ul>
     *   <li>Toma a maior paymentDate das parcelas existentes</li>
     *   <li>Soma 30 dias (próxima parcela mensal)</li>
     *   <li>Se ultrapassar o eventDate, usa o eventDate como teto</li>
     *   <li>Se não houver parcelas com data, usa o eventDate diretamente</li>
     * </ul>
     *
     * @return lista original se não houver déficit; lista aumentada com a nova parcela se houver
     */
    List<RentalPaymentInputDTO> autoCompletePayments(
            List<RentalPaymentInputDTO> payments,
            List<ContractItemInputDTO> items,
            LocalDate eventDate) {

        java.math.BigDecimal deficit = validator.calculatePaymentDeficit(payments, items);
        if (deficit.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return payments; // sem déficit (ou zero) — nada a fazer
        }

        // Calcula o próximo installmentNumber
        int maxInstallmentNumber = payments.stream()
                .map(RentalPaymentInputDTO::installmentNumber)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        int nextInstallmentNumber = maxInstallmentNumber + 1;

        if (nextInstallmentNumber > 24) {
            throw new br.com.rentafit.common.exception.ValidationException(
                    "Não é possível criar parcela automática: limite máximo de 24 parcelas atingido");
        }

        // Calcula a próxima data compatível
        LocalDate nextPaymentDate = computeNextPaymentDate(payments, eventDate);

        RentalPaymentInputDTO autoPayment = new RentalPaymentInputDTO(
                nextInstallmentNumber,
                nextPaymentDate,
                "PIX",
                deficit,
                1,
                null,
                "PENDING"
        );

        List<RentalPaymentInputDTO> augmented = new java.util.ArrayList<>(payments);
        augmented.add(autoPayment);

        log.info("Auto-created PENDING installment #{} (R$ {}) with date {} to cover contract deficit",
                nextInstallmentNumber, deficit, nextPaymentDate);

        return augmented;
    }

    /**
     * Calcula a próxima data de pagamento compatível:
     * maxPaymentDate + 30 dias, limitado ao eventDate.
     */
    private LocalDate computeNextPaymentDate(List<RentalPaymentInputDTO> payments, LocalDate eventDate) {
        LocalDate latestPaymentDate = payments.stream()
                .map(RentalPaymentInputDTO::paymentDate)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        if (latestPaymentDate == null) {
            return eventDate;
        }

        LocalDate candidate = latestPaymentDate.plusDays(30);
        return candidate.isAfter(eventDate) ? eventDate : candidate;
    }

    /**
     * Gera legacyId no formato YYYYMMDD-N, onde N é sequencial no dia.
     * <p>IDs legados importados futuramente serão inteiros simples (ex: "1", "2"),
     * sem conflito com este formato.</p>
     */
    String generateLegacyId() {
        String prefix = LocalDate.now().format(LEGACY_ID_DATE_FMT) + "-";
        return contractRepository.findMaxLegacyIdByPrefix(prefix)
                .map(max -> {
                    int lastN = Integer.parseInt(max.substring(prefix.length()));
                    return prefix + (lastN + 1);
                })
                .orElse(prefix + "1");
    }

    private RentalContract requireContract(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RentalContract", "id", id.toString()));
    }
    private RentalContract requireContractByLegacyId(String legacy) {
        return contractRepository.findByLegacyId(legacy)
                .orElseThrow(() -> new ResourceNotFoundException("RentalContract", "legacyId", legacy));
    }

    private void requireStatus(RentalContract contract, ContractStatus expected, String action) {
        if (!expected.equals(contract.getStatus())) {
            throw new ValidationException(
                    "Não é possível " + action + " um contrato em status " + contract.getStatus()
                            + ". Status esperado: " + expected);
        }
    }

    private void requireStatusOneOf(RentalContract contract, String action, ContractStatus... allowed) {
        for (ContractStatus s : allowed) {
            if (s.equals(contract.getStatus())) return;
        }
        throw new ValidationException(
                "Não é possível " + action + " um contrato em status " + contract.getStatus()
                        + ". Status esperado: " + java.util.Arrays.stream(allowed)
                        .map(Enum::name).collect(Collectors.joining(" ou ")));
    }
}

