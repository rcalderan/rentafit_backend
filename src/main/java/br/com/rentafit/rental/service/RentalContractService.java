package br.com.rentafit.rental.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.common.search.SearchMode;
import br.com.rentafit.common.search.TsQueryBuilder;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.*;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.port.CustomerPort;
import br.com.rentafit.rental.port.CustomerPort.CustomerSnapshot;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.validation.RentalContractValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
 *   <li>Conflitos de reserva bloqueiam já no salvamento (create/update)
 *   e são revalidados com lock em sign() e finalize().</li>
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
    private final CustomerPort customerPort;
    private final RentalMapper mapper;
    private final RentalRevisionService revisionService;
    private final RentalProposalDuplication proposalDuplication;

    @Value("${rentafit.legacy-id.pattern:yyMMdd}")
    private String legacyIdPattern;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<RentalContractSummaryDTO> findAll(Pageable pageable) {
        return toSummaryPage(contractRepository.findAll(pageable));
    }

    @Transactional(readOnly = true)
    public Page<RentalContractSummaryDTO> search(String q, SearchMode mode, Pageable pageable) {
        String tsQuery = TsQueryBuilder.toTsQuery(q, mode);
        if (tsQuery == null) {
            return findAll(pageable);
        }
        return toSummaryPage(contractRepository.searchByFullText(tsQuery, TsQueryBuilder.toExactQuery(q), pageable));
    }

    @Transactional(readOnly = true)
    public RentalContractDetailsDTO findById(UUID id) {
        RentalContract contract = requireContract(id);
        return enrichBlankCustomerDocument(mapper.toDetailsDTO(contract, null));
    }

    @Transactional(readOnly = true)
    public RentalContractDetailsDTO findByLegacyId(String leg, Integer installmentNumber) {
        RentalContract contract = requireContractByLegacyId(leg);
        List<RentalPayment> payments = installmentNumber == null
                ? contract.getPayments()
                : contract.getPayments().stream()
                        .filter(p -> installmentNumber.equals(p.getInstallmentNumber()))
                        .collect(Collectors.toList());
        return enrichBlankCustomerDocument(mapper.toDetailsDTO(contract, payments, null));
    }

    @Transactional(readOnly = true)
    public Page<RentalContractSummaryDTO> findByCustomer(UUID customerId, Pageable pageable) {
        return toSummaryPage(contractRepository.findByCustomerId(customerId, pageable));
    }

    /**
     * Converte uma página de contratos em DTOs de listagem sem tocar nas coleções
     * LAZY (SUBSELECT sem restrição ignoraria o LIMIT e carregaria as tabelas
     * inteiras). Totais vêm de 2 agregações GROUP BY restritas aos IDs da página.
     */
    private Page<RentalContractSummaryDTO> toSummaryPage(Page<RentalContract> page) {
        List<UUID> ids = page.getContent().stream().map(RentalContract::getId).toList();
        if (ids.isEmpty()) {
            return page.map(c -> mapper.toSummaryDTO(c, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        Map<UUID, BigDecimal> totals = contractRepository.sumItemValuesByContractIds(ids).stream()
                .collect(Collectors.toMap(
                        RentalContractRepository.ContractValueTotal::getContractId,
                        RentalContractRepository.ContractValueTotal::getTotal));
        Map<UUID, BigDecimal> paid = contractRepository.sumPaidValuesByContractIds(ids).stream()
                .collect(Collectors.toMap(
                        RentalContractRepository.ContractValueTotal::getContractId,
                        RentalContractRepository.ContractValueTotal::getTotal));
        return page.map(c -> mapper.toSummaryDTO(
                c,
                totals.getOrDefault(c.getId(), BigDecimal.ZERO),
                paid.getOrDefault(c.getId(), BigDecimal.ZERO)));
    }

    /**
     * Enriquece o DTO de leitura com o CPF/CNPJ atual do cliente quando o snapshot
     * imutável gravado na criação do contrato está em branco (contratos legados).
     * Não muta a entidade — apenas o DTO de saída.
     */
    private RentalContractDetailsDTO enrichBlankCustomerDocument(RentalContractDetailsDTO dto) {
        if (dto.customerDocument() != null && !dto.customerDocument().isBlank()) {
            return dto;
        }
        return customerPort.findById(dto.customerId())
                .map(CustomerSnapshot::document)
                .filter(doc -> doc != null && !doc.isBlank())
                .map(currentDoc -> {
                    log.info("Enriquecido customerDocument em branco do contrato {} com documento atual do cliente {}",
                            dto.id(), dto.customerId());
                    return dto.toBuilder().customerDocument(currentDoc).build();
                })
                .orElse(dto);
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

        // Bloqueio antecipado: mesma janela de conflito aplicada em sign(), já no salvar
        validator.validateNoReservationConflicts(dto.items(), dto.eventDate(), null, null);

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
        contractRepository.lockById(id);
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
        validator.validateAccessoriesAvailability(revisionService.pendingAccessoryIds(contract, accessoryIds));

        // Bloqueio antecipado no salvar; em REVISION o contrato original é excluído da checagem
        validator.validateNoReservationConflicts(dto.items(), dto.eventDate(), contract.getId(), contract.getParentContractId());

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
        return sign(id, null);
    }

    public RentalContractDetailsDTO sign(UUID id, String printTemplateId) {
        RentalContract contract = requireContract(id);
        if (contract.getParentContractId() != null) contractRepository.lockById(contract.getParentContractId());
        contractRepository.lockById(id);
        if (contract.getParentContractId() != null) {
            var parent = requireContract(contract.getParentContractId());
            validator.lockItemsForTransition(java.util.stream.Stream.concat(parent.getItems().stream(), contract.getItems().stream()).toList());
        }
        requireStatusOneOf(contract, "assinar", ContractStatus.DRAFT, ContractStatus.REVISION);
        validator.validateItemsAvailability(contract.getItems().stream().map(RentalContractItem::getRentalItemId).toList());

        validator.validateDateOrder(contract.getPickupDate(), contract.getEventDate(), contract.getReturnDate());
        validator.validatePersistedPaidPaymentsHaveEmployee(contract.getPayments());

        List<String> warnings = validator.checkConflictsForTransition(
                contract.getItems(), contract.getEventDate(), contract.getId());

        if (printTemplateId != null && !printTemplateId.isBlank()) {
            contract.setPrintTemplateId(printTemplateId);
        }
        contract.setStatus(ContractStatus.SIGNED);
        RentalContract saved = contractRepository.save(contract);

        // Supersede the parent contract if this is a revision
        if (saved.getParentContractId() != null) {
            revisionService.confirm(saved);
            log.info("Parent contract {} superseded by revision {}", saved.getParentContractId(), saved.getId());
        }

        log.info("Contract {} signed. Warnings: {}", id, warnings != null ? warnings.size() : 0);
        return mapper.toDetailsDTO(saved, warnings);
    }

    /**
     * SIGNED → FINALIZED.
     * Re-valida conflitos e aciona o workflow de reserva.
     */
    public RentalContractDetailsDTO finalize(UUID id) {
        contractRepository.lockById(id);
        RentalContract contract = requireContract(id);
        requireStatus(contract, ContractStatus.SIGNED, "finalizar");
        validator.validateItemsAvailability(contract.getItems().stream().map(RentalContractItem::getRentalItemId).toList());

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
        contractRepository.lockById(contractId);
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
     * Processa a devolução do contrato (operação legada — bulk).
     *
     * @deprecated Substituído pelo sistema de devolução granular ({@link ReturnService}).
     *     Mantido para compatibilidade com integrações existentes.
     *     Usar {@code POST /api/v1/rental/contracts/{id}/return-mark} e
     *     {@code POST /api/v1/rental/contracts/{id}/return-close} para novos fluxos.
     */
    @Deprecated(since = "2.0", forRemoval = false)
    public RentalContractDetailsDTO processReturn(UUID id, ReturnContractDTO dto) {
        contractRepository.lockById(id);
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
        return revisionService.create(id, this::generateLegacyId);
    }

    /**
     * Duplica um contrato como novo DRAFT.
     * Snapshot do cliente é atualizado; itens são copiados; pagamentos são zerados.
     */
    public RentalContractDetailsDTO duplicate(UUID id) {
        return proposalDuplication.create(requireContract(id), this::generateLegacyId);
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
        contractRepository.lockLegacyIdGeneration();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(legacyIdPattern);
        String prefix = LocalDate.now().format(formatter) + "-";
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

