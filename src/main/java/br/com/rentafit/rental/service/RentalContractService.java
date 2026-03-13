package br.com.rentafit.rental.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
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

import java.util.List;
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
    public Page<RentalContractSummaryDTO> findByCustomer(UUID customerId, Pageable pageable) {
        return contractRepository.findByCustomerId(customerId, pageable).map(mapper::toSummaryDTO);
    }

    public RentalContractDetailsDTO create(CreateRentalContractDTO dto) {
        // Validação básica de datas
        validator.validateDateOrder(dto.pickupDate(), dto.eventDate(), dto.returnDate());

        // Valida e obtém snapshot do cliente
        CustomerSnapshot snapshot = validator.validateAndGetCustomer(dto.customerId());

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

        // Valida que as parcelas somam o valor total dos itens
        validator.validatePaymentsMatchTotal(dto.payments(), dto.items());

        RentalContract contract = mapper.toEntity(dto, snapshot);
        RentalContract saved = contractRepository.saveAndFlush(contract);
        log.info("Created rental contract {} for customer {}", saved.getId(), snapshot.id());
        return mapper.toDetailsDTO(saved, null);
    }

    public RentalContractDetailsDTO update(UUID id, UpdateRentalContractDTO dto) {
        RentalContract contract = requireContract(id);

        if (!ContractStatus.DRAFT.equals(contract.getStatus())) {
            throw new ValidationException("Apenas contratos em DRAFT podem ser atualizados. Status atual: " + contract.getStatus());
        }

        validator.validateDateOrder(dto.pickupDate(), dto.eventDate(), dto.returnDate());

        List<UUID> rentalItemIds = dto.items().stream()
                .map(ContractItemInputDTO::rentalItemId).collect(Collectors.toList());
        validator.validateItemsAvailability(rentalItemIds);

        List<UUID> accessoryIds = dto.items().stream()
                .filter(i -> i.metadata() != null)
                .flatMap(i -> i.metadata().stream())
                .map(ContractItemMetaInputDTO::accessoryId)
                .collect(Collectors.toList());
        validator.validateAccessoriesAvailability(accessoryIds);

        // Valida que as parcelas somam o valor total dos itens
        validator.validatePaymentsMatchTotal(dto.payments(), dto.items());

        mapper.updateEntityFromDTO(contract, dto);
        RentalContract saved = contractRepository.save(contract);
        log.info("Updated rental contract {}", id);
        return mapper.toDetailsDTO(saved, null);
    }

    // ── Transições de Estado ──────────────────────────────────────────────────

    /**
     * DRAFT → SIGNED.
     * Executa checagem de conflitos. BLOCKING → 422; WARNING → retornado no DTO.
     */
    public RentalContractDetailsDTO sign(UUID id) {
        RentalContract contract = requireContract(id);
        requireStatus(contract, ContractStatus.DRAFT, "assinar");

        validator.validateDateOrder(contract.getPickupDate(), contract.getEventDate(), contract.getReturnDate());

        List<String> warnings = validator.checkConflictsForTransition(
                contract.getItems(), contract.getEventDate(), contract.getId());

        contract.setStatus(ContractStatus.SIGNED);
        RentalContract saved = contractRepository.save(contract);
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

        RentalContract saved = contractRepository.save(duplicate);
        log.info("Contract {} duplicated as {}", id, saved.getId());
        return mapper.toDetailsDTO(saved, null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RentalContract requireContract(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RentalContract", "id", id.toString()));
    }

    private void requireStatus(RentalContract contract, ContractStatus expected, String action) {
        if (!expected.equals(contract.getStatus())) {
            throw new ValidationException(
                    "Não é possível " + action + " um contrato em status " + contract.getStatus()
                            + ". Status esperado: " + expected);
        }
    }
}

