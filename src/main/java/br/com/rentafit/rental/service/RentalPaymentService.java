package br.com.rentafit.rental.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.RentalPaymentDetailsDTO;
import br.com.rentafit.rental.dto.RentalPaymentInputDTO;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.validation.RentalContractValidator;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.repository.RentalPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gerencia as parcelas de pagamento de um contrato.
 *
 * <p>Regras de negócio:
 * <ul>
 *   <li>addPayment: sempre permitido, mesmo após FINALIZED.</li>
 *   <li>Parcelas PAID não podem ser alteradas/canceladas após contrato assinado (SIGNED/FINALIZED).</li>
 *   <li>Parcelas não pagas podem ser alteradas mesmo após SIGNED/FINALIZED.</li>
 *   <li>paymentDate não pode ser posterior ao eventDate do contrato.</li>
 *   <li>Soma PENDING+PAID não pode ultrapassar totalValue dos itens.</li>
 *   <li>Máximo 24 parcelas por contrato.</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RentalPaymentService {

    private static final int MAX_INSTALLMENTS = 24;

    private final RentalPaymentRepository paymentRepository;
    private final RentalContractRepository contractRepository;
    private final RentalContractValidator validator;
    private final RentalMapper mapper;

    public List<RentalPaymentDetailsDTO> listByContract(UUID contractId) {
        requireContractExists(contractId);
        return paymentRepository.findByContractIdOrderByInstallmentNumber(contractId)
                .stream().map(mapper::toPaymentDetailsDTO).collect(Collectors.toList());
    }

    public RentalPaymentDetailsDTO addPayment(UUID contractId, RentalPaymentInputDTO dto) {
        RentalContract contract = requireContract(contractId);

        validatePaymentDate(dto, contract);
        validateInstallmentLimit(contractId);
        validateTotalValueNotExceeded(contractId, dto.value(), null, contract);
        validator.validateSinglePaidPaymentHasEmployee(dto);

        RentalPayment payment = mapper.toPaymentEntity(dto, contract);
        RentalPayment saved = paymentRepository.save(payment);
        log.info("Payment added to contract {}: installment #{}", contractId, saved.getInstallmentNumber());
        return mapper.toPaymentDetailsDTO(saved);
    }

    /**
     * Atualiza uma parcela existente.
     *
     * <p>Se a alteração reduzir o valor comprometido (PENDING+PAID) abaixo do totalValue
     * do contrato, uma nova parcela PENDING é criada automaticamente para cobrir o déficit.</p>
     *
     * @return lista contendo a parcela atualizada e, se houver, a parcela-gap criada automaticamente
     */
    public List<RentalPaymentDetailsDTO> updatePayment(UUID contractId, UUID paymentId, RentalPaymentInputDTO dto) {
        RentalPayment payment = requirePayment(paymentId, contractId);
        RentalContract contract = requireContract(contractId);
        validatePaidInstallmentMutationAllowed(contract, payment, "atualizar");
        //validateLockedContractSettlementIntegrity(contract, payment, dto);
        //validatePaymentDate(dto, contract);
        validateTotalValueNotExceeded(contractId, dto.value(), payment, contract);
        validator.validateSinglePaidPaymentHasEmployee(dto);

        payment.setPaymentDate(dto.paymentDate());
        payment.setInstallmentNumber(dto.installmentNumber());
        payment.setPaymentMethod(PaymentMethod.valueOf(dto.paymentMethod().toUpperCase()));
        payment.setValue(dto.value());
        payment.setInstallments(dto.installments() != null ? dto.installments() : 1);
        payment.setProcessedByEmployeeId(dto.processedByEmployeeId());
        if (dto.status() != null) {
            payment.setStatus(PaymentStatus.valueOf(dto.status().toUpperCase()));
        }

        RentalPayment saved = paymentRepository.save(payment);
        log.info("Payment {} updated in contract {}", paymentId, contractId);

        List<RentalPaymentDetailsDTO> result = new ArrayList<>();
        result.add(mapper.toPaymentDetailsDTO(saved));

        // Auto-create gap payment if the update created a deficit
        RentalPayment gapPayment = autoCreateGapPaymentIfNeeded(contract);
        if (gapPayment != null) {
            result.add(mapper.toPaymentDetailsDTO(gapPayment));
        }

        return result;
    }

    public void cancelPayment(UUID contractId, UUID paymentId) {
        RentalContract contract = requireContract(contractId);

        RentalPayment payment = requirePayment(paymentId, contractId);
        validatePaidInstallmentMutationAllowed(contract, payment, "cancelar");
        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
        log.info("Payment {} cancelled in contract {}", paymentId, contractId);
    }

    // ── Auto-gap ───────────────────────────────────────────────────────────────

    /**
     * Verifica se existe déficit entre o totalValue do contrato e a soma PENDING+PAID.
     * Se existir, cria automaticamente uma parcela PENDING para cobrir a diferença.
     *
     * @return a parcela-gap criada, ou null se não houver déficit
     */
    RentalPayment autoCreateGapPaymentIfNeeded(RentalContract contract) {
        UUID contractId = contract.getId();

        BigDecimal totalItems = contract.getItems().stream()
                .map(i -> i.getValue() != null ? i.getValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentCommitted = paymentRepository.sumValueByContractIdAndStatusIn(
                contractId, List.of(PaymentStatus.PENDING, PaymentStatus.PAID));

        BigDecimal deficit = totalItems.subtract(currentCommitted);
        if (deficit.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // sem déficit
        }

        // Validate installment limit before auto-creating
        long activeCount = paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED);
        if (activeCount >= MAX_INSTALLMENTS) {
            log.warn("Cannot auto-create gap payment for contract {}: limit of {} installments reached. Deficit: R$ {}",
                    contractId, MAX_INSTALLMENTS, deficit);
            return null;
        }

        int nextInstallmentNumber = paymentRepository.findMaxInstallmentNumberByContractId(contractId) + 1;
        LocalDate nextPaymentDate = computeNextPaymentDate(contractId, contract.getEventDate());

        RentalPayment gapPayment = RentalPayment.builder()
                .contract(contract)
                .installmentNumber(nextInstallmentNumber)
                .paymentDate(nextPaymentDate)
                .paymentMethod(PaymentMethod.PIX)
                .value(deficit)
                .installments(1)
                .status(PaymentStatus.PENDING)
                .build();

        RentalPayment saved = paymentRepository.save(gapPayment);
        log.info("Auto-created gap payment #{} (R$ {}) for contract {} to cover deficit",
                nextInstallmentNumber, deficit, contractId);
        return saved;
    }

    /**
     * Calcula a próxima data de pagamento: max(paymentDate) + 30 dias, limitada ao eventDate.
     */
    private LocalDate computeNextPaymentDate(UUID contractId, LocalDate eventDate) {
        LocalDate latestPaymentDate = paymentRepository.findMaxPaymentDateByContractId(contractId)
                .orElse(null);

        if (latestPaymentDate == null) {
            return eventDate;
        }

        LocalDate candidate = latestPaymentDate.plusDays(30);
        return (eventDate != null && candidate.isAfter(eventDate)) ? eventDate : candidate;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validatePaymentDate(RentalPaymentInputDTO dto, RentalContract contract) {
        LocalDate today = LocalDate.now();

        if (dto.paymentDate() != null && dto.paymentDate().isBefore(today)) {
            throw new ValidationException(
                    "Data do pagamento (" + dto.paymentDate() + ") não pode ser anterior à data atual ("
                            + today + ")");
        }

        if (dto.paymentDate() != null && contract.getEventDate() != null
                && dto.paymentDate().isAfter(contract.getEventDate())) {
            throw new ValidationException(
                    "Data do pagamento (" + dto.paymentDate() + ") não pode ser posterior à data do evento ("
                            + contract.getEventDate() + ")");
        }
    }

    private void validateInstallmentLimit(UUID contractId) {
        long count = paymentRepository.countByContractIdAndStatusNot(contractId, PaymentStatus.CANCELLED);
        if (count >= MAX_INSTALLMENTS) {
            throw new ValidationException("Limite máximo de " + MAX_INSTALLMENTS + " parcelas atingido");
        }
    }

    private void validateTotalValueNotExceeded(UUID contractId, BigDecimal newValue, RentalPayment existingPayment, RentalContract contract) {
        BigDecimal totalItems = contract.getItems().stream()
                .map(i -> i.getValue() != null ? i.getValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentCommitted = paymentRepository.sumValueByContractIdAndStatusIn(
                contractId, List.of(PaymentStatus.PENDING, PaymentStatus.PAID));

        // When updating an existing payment, subtract its current value to avoid double-counting.
        // Only subtract if the payment was PENDING or PAID (i.e., it was included in the sum above).
        if (existingPayment != null
                && existingPayment.getValue() != null
                && (PaymentStatus.PENDING.equals(existingPayment.getStatus())
                    || PaymentStatus.PAID.equals(existingPayment.getStatus()))) {
            currentCommitted = currentCommitted.subtract(existingPayment.getValue());
        }

        BigDecimal projected = currentCommitted.add(newValue != null ? newValue : BigDecimal.ZERO);
        if (projected.compareTo(totalItems) > 0) {
            throw new ValidationException(
                    "Valor total dos pagamentos (" + projected + ") ultrapassa o valor total do contrato (" + totalItems + ")");
        }
    }

    private void validatePaidInstallmentMutationAllowed(RentalContract contract, RentalPayment payment, String action) {
        boolean contractLocked = ContractStatus.SIGNED.equals(contract.getStatus())
                || ContractStatus.FINALIZED.equals(contract.getStatus())
                || ContractStatus.REVISION.equals(contract.getStatus())
                || ContractStatus.CLOSED.equals(contract.getStatus());

        if (contractLocked && PaymentStatus.PAID.equals(payment.getStatus())) {
            throw new ValidationException(
                    "Não é possível " + action + " parcela PAGA após contrato assinado/finalizado/em revisão");
        }
    }

    /**
     * Em contratos assinados/finalizados/em revisão, marcar uma parcela como PAID
     * não pode alterar os dados financeiros originais da parcela (número, data,
     * forma ou valor). Isso evita mutação retroativa silenciosa durante a baixa.
     */
    private void validateLockedContractSettlementIntegrity(RentalContract contract, RentalPayment existingPayment,
                                                           RentalPaymentInputDTO dto) {
        boolean contractLocked = ContractStatus.SIGNED.equals(contract.getStatus())
                || ContractStatus.FINALIZED.equals(contract.getStatus())
                || ContractStatus.REVISION.equals(contract.getStatus())
                || ContractStatus.CLOSED.equals(contract.getStatus());

        boolean existingIsPaid = PaymentStatus.PAID.equals(existingPayment.getStatus());
        boolean incomingIsPaid = dto.status() != null && "PAID".equalsIgnoreCase(dto.status());
        boolean settlingNow = !existingIsPaid && incomingIsPaid;

        if (!contractLocked || !settlingNow) {
            return;
        }

        boolean installmentChanged = dto.installmentNumber() != null
                && !dto.installmentNumber().equals(existingPayment.getInstallmentNumber());
        boolean dateChanged = dto.paymentDate() != null
                && !dto.paymentDate().equals(existingPayment.getPaymentDate());
        boolean methodChanged = dto.paymentMethod() != null
                && !existingPayment.getPaymentMethod().name().equalsIgnoreCase(dto.paymentMethod());
        boolean valueChanged = dto.value() != null
                && existingPayment.getValue() != null
                && dto.value().compareTo(existingPayment.getValue()) != 0;

        if (installmentChanged || dateChanged || methodChanged || valueChanged) {
            throw new ValidationException(
                    "Ao marcar parcela como PAGA em contrato assinado/finalizado/em revisão, "
                            + "não é permitido alterar número, data, forma ou valor da parcela");
        }
    }

    private RentalContract requireContract(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalContract", "id", contractId.toString()));
    }

    private void requireContractExists(UUID contractId) {
        if (!contractRepository.existsById(contractId)) {
            throw new ResourceNotFoundException("RentalContract", "id", contractId.toString());
        }
    }

    private RentalPayment requirePayment(UUID paymentId, UUID contractId) {
        return paymentRepository.findByIdAndContractId(paymentId, contractId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalPayment", "id", paymentId.toString()));
    }
}

