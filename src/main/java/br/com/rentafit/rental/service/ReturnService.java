package br.com.rentafit.rental.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.repository.EmployeeRepository;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.ItemMetaType;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.CloseReturnRequestDTO;
import br.com.rentafit.rental.dto.MarkReturnRequestDTO;
import br.com.rentafit.rental.dto.ReturnEntryDTO;
import br.com.rentafit.rental.dto.ReturnSummaryDTO;
import br.com.rentafit.rental.repository.RentalContractItemMetaRepository;
import br.com.rentafit.rental.repository.RentalContractItemRepository;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.repository.RentalPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gerencia o ciclo de devolução granular de contratos de locação.
 *
 * <p>Fluxo: FINALIZED → (markItemsReturned)* → (closeReturn) → CLOSED</p>
 *
 * <p>O fechamento reutiliza {@code RentalWorkflowService#onReturn} para liberar
 * estoque e mover itens para MAINTENANCE — preservando a lógica existente.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReturnService {

    private final RentalContractRepository contractRepository;
    private final RentalContractItemRepository itemRepository;
    private final RentalContractItemMetaRepository metaRepository;
    private final RentalPaymentRepository paymentRepository;
    private final EmployeeRepository employeeRepository;
    private final RentalWorkflowService workflowService;

    // ── Consulta ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReturnSummaryDTO getReturnSummary(UUID contractId) {
        RentalContract contract = requireFinalized(contractId);
        return toSummaryDTO(contract);
    }

    // ── Marcação de itens ─────────────────────────────────────────────────────

    public ReturnSummaryDTO markItemsReturned(UUID contractId, MarkReturnRequestDTO dto) {
        RentalContract contract = requireFinalized(contractId);

        for (ReturnEntryDTO entry : dto.entries()) {
            if (entry.accessoryId() == null) {
                markItemReturned(contract, entry, dto.returnerName());
            } else {
                markAccessoryReturned(contract, entry);
            }
        }

        if (Boolean.TRUE.equals(dto.applyFine()) && dto.fineAmount() != null && dto.fineAmount().compareTo(BigDecimal.ZERO) > 0) {
            createFinePayment(contract, dto.fineAmount(), null);
            log.info("Fine payment of {} created during mark for contract {}", dto.fineAmount(), contractId);
        }

        RentalContract refreshed = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalContract", "id", contractId.toString()));
        log.info("Marked {} return entries for contract {}", dto.entries().size(), contractId);
        return toSummaryDTO(refreshed);
    }

    // ── Fechamento ────────────────────────────────────────────────────────────

    public ReturnSummaryDTO closeReturn(UUID contractId, CloseReturnRequestDTO dto) {
        RentalContract contract = requireFinalized(contractId);

        validateAllItemsReturned(contract);
        validateAllPaymentsPaid(contract);
        validateEmployee(dto.employeeId());

        if (Boolean.TRUE.equals(dto.applyFine()) && dto.fineAmount() != null && dto.fineAmount().compareTo(BigDecimal.ZERO) > 0) {
            createFinePayment(contract, dto.fineAmount(), dto.employeeId());
        }

        contract.setStatus(ContractStatus.CLOSED);
        contract.setReturned(true);
        contract.setActualReturnDate(LocalDate.now());
        contract.setReturnedByEmployeeId(dto.employeeId());
        RentalContract saved = contractRepository.save(contract);

        workflowService.onReturn(saved);
        log.info("Contract {} closed by employee {}", contractId, dto.employeeId());
        return toSummaryDTO(saved);
    }

    // ── Helpers de negócio ────────────────────────────────────────────────────

    private void markItemReturned(RentalContract contract, ReturnEntryDTO entry, String returnerName) {
        RentalContractItem item = contract.getItems().stream()
                .filter(i -> i.getId().equals(entry.itemId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RentalContractItem", "id", entry.itemId().toString()));

        if (Boolean.TRUE.equals(item.getReturned())) {
            return;
        }

        item.setReturned(true);
        item.setReturnedAt(entry.returnedAt());
        item.setReturnedByName(returnerName);
        itemRepository.save(item);
    }

    private void markAccessoryReturned(RentalContract contract, ReturnEntryDTO entry) {
        RentalContractItem item = contract.getItems().stream()
                .filter(i -> i.getId().equals(entry.itemId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RentalContractItem", "id", entry.itemId().toString()));

        RentalContractItemMeta meta = item.getMetadata().stream()
                .filter(m -> ItemMetaType.ACESSORIO.equals(m.getType())
                        && entry.accessoryId().equals(m.getAccessoryId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RentalContractItemMeta", "accessoryId", entry.accessoryId().toString()));

        if (Boolean.TRUE.equals(meta.getReturned())) {
            return;
        }

        meta.setReturned(true);
        meta.setReturnedAt(entry.returnedAt());
        metaRepository.save(meta);
    }

    private void validateAllItemsReturned(RentalContract contract) {
        boolean hasUnreturnedItems = contract.getItems().stream()
                .anyMatch(i -> !Boolean.TRUE.equals(i.getReturned()));

        if (hasUnreturnedItems) {
            throw new ValidationException("Todos os itens devem ser devolvidos antes de fechar o contrato");
        }

        boolean hasUnreturnedAccessories = contract.getItems().stream()
                .flatMap(i -> i.getMetadata().stream())
                .filter(m -> ItemMetaType.ACESSORIO.equals(m.getType()))
                .anyMatch(m -> !Boolean.TRUE.equals(m.getReturned()));

        if (hasUnreturnedAccessories) {
            throw new ValidationException("Todos os acessórios devem ser devolvidos antes de fechar o contrato");
        }
    }

    private void validateAllPaymentsPaid(RentalContract contract) {
        boolean hasPendingPayments = contract.getPayments().stream()
                .anyMatch(p -> PaymentStatus.PENDING.equals(p.getStatus()));

        if (hasPendingPayments) {
            long count = contract.getPayments().stream()
                    .filter(p -> PaymentStatus.PENDING.equals(p.getStatus()))
                    .count();
            throw new ValidationException(
                    "O contrato possui " + count + " parcela(s) pendente(s). Quite todos os pagamentos antes de fechar.");
        }
    }

    private void validateEmployee(UUID employeeId) {
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));
    }

    /**
     * Calcula a multa sugerida por atraso.
     *
     * <p>Regra: 1% do valor total do contrato por dia de atraso, limitado a 20% do total.
     * Retorna ZERO quando não há atraso ou o contrato já foi devolvido.</p>
     */
    private BigDecimal calculateSuggestedFine(RentalContract contract, long delayDays) {
        if (delayDays <= 0 || Boolean.TRUE.equals(contract.getReturned())) {
            return BigDecimal.ZERO;
        }
        BigDecimal contractTotal = contract.getItems().stream()
                .map(RentalContractItem::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (contractTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // 1% por dia de atraso, teto de 20%
        BigDecimal dailyRate = BigDecimal.valueOf(0.01);
        BigDecimal maxRate = BigDecimal.valueOf(0.20);
        BigDecimal appliedRate = dailyRate.multiply(BigDecimal.valueOf(delayDays)).min(maxRate);
        return contractTotal.multiply(appliedRate).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void createFinePayment(RentalContract contract, BigDecimal fineAmount, UUID employeeId) {
        int nextInstallment = paymentRepository
                .findMaxInstallmentNumberByContractId(contract.getId()) + 1;

        RentalPayment fine = RentalPayment.builder()
                .contract(contract)
                .installmentNumber(nextInstallment)
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.CASH)
                .value(fineAmount)
                .installments(1)
                .status(PaymentStatus.MULTA)
                .processedByEmployeeId(employeeId)
                .build();

        paymentRepository.save(fine);
        log.info("Fine payment of {} created for contract {}", fineAmount, contract.getId());
    }

    // ── Mapeamento ────────────────────────────────────────────────────────────

    private ReturnSummaryDTO toSummaryDTO(RentalContract contract) {
        long delayDays = Math.max(0, ChronoUnit.DAYS.between(contract.getReturnDate(), LocalDate.now()));

        List<ReturnSummaryDTO.ReturnItemDTO> items = contract.getItems().stream()
                .map(this::toReturnItemDTO)
                .collect(Collectors.toList());

        List<ReturnSummaryDTO.ReturnPaymentPreviewDTO> paymentsPreview = contract.getPayments().stream()
                .map(p -> ReturnSummaryDTO.ReturnPaymentPreviewDTO.builder()
                        .installmentNumber(p.getInstallmentNumber())
                        .value(p.getValue())
                        .status(p.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        long pendingCount = items.stream()
                .filter(i -> !i.isReturned())
                .count()
                + items.stream()
                .flatMap(i -> i.accessories().stream())
                .filter(a -> !a.isReturned())
                .count();

        boolean isFullyReturned = pendingCount == 0;

        return ReturnSummaryDTO.builder()
                .contractId(contract.getId())
                .legacyId(contract.getLegacyId())
                .customerName(contract.getCustomerName())
                .returnDate(contract.getReturnDate())
                .actualReturnDate(contract.getActualReturnDate())
                .pendingCount((int) pendingCount)
                .isFullyReturned(isFullyReturned)
                .delayDays(delayDays)
                .suggestedFine(calculateSuggestedFine(contract, delayDays))
                .items(items)
                .paymentsPreview(paymentsPreview)
                .build();
    }

    private ReturnSummaryDTO.ReturnItemDTO toReturnItemDTO(RentalContractItem item) {
        List<ReturnSummaryDTO.ReturnAccessoryDTO> accessories = item.getMetadata().stream()
                .filter(m -> ItemMetaType.ACESSORIO.equals(m.getType()))
                .map(m -> ReturnSummaryDTO.ReturnAccessoryDTO.builder()
                        .accessoryId(m.getId())
                        .description(m.getDescription())
                        .isReturned(Boolean.TRUE.equals(m.getReturned()))
                        .returnedAt(m.getReturnedAt() != null ? m.getReturnedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());

        return ReturnSummaryDTO.ReturnItemDTO.builder()
                .itemId(item.getId())
                .description(item.getDescription())
                .isReturned(Boolean.TRUE.equals(item.getReturned()))
                .returnedAt(item.getReturnedAt() != null ? item.getReturnedAt().toString() : null)
                .returnedBy(item.getReturnedByName())
                .accessories(accessories)
                .build();
    }

    private RentalContract requireFinalized(UUID contractId) {
        RentalContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalContract", "id", contractId.toString()));

        if (!ContractStatus.FINALIZED.equals(contract.getStatus())) {
            throw new ValidationException(
                    "Operação disponível apenas para contratos FINALIZED. Status atual: " + contract.getStatus());
        }

        return contract;
    }
}
