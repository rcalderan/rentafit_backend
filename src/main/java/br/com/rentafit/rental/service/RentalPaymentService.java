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

    public RentalPaymentDetailsDTO updatePayment(UUID contractId, UUID paymentId, RentalPaymentInputDTO dto) {
        RentalContract contract = requireContract(contractId);

        RentalPayment payment = requirePayment(paymentId, contractId);
        validatePaidInstallmentMutationAllowed(contract, payment, "atualizar");
        validatePaymentDate(dto, contract);
        validateTotalValueNotExceeded(contractId, dto.value(), payment, contract);
        validator.validateSinglePaidPaymentHasEmployee(dto);

        payment.setInstallmentNumber(dto.installmentNumber());
        payment.setPaymentDate(dto.paymentDate());
        payment.setPaymentMethod(PaymentMethod.valueOf(dto.paymentMethod().toUpperCase()));
        payment.setValue(dto.value());
        payment.setInstallments(dto.installments() != null ? dto.installments() : 1);
        payment.setProcessedByEmployeeId(dto.processedByEmployeeId());
        if (dto.status() != null) {
            payment.setStatus(PaymentStatus.valueOf(dto.status().toUpperCase()));
        }

        RentalPayment saved = paymentRepository.save(payment);
        log.info("Payment {} updated in contract {}", paymentId, contractId);
        return mapper.toPaymentDetailsDTO(saved);
    }

    public void cancelPayment(UUID contractId, UUID paymentId) {
        RentalContract contract = requireContract(contractId);

        RentalPayment payment = requirePayment(paymentId, contractId);
        validatePaidInstallmentMutationAllowed(contract, payment, "cancelar");
        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
        log.info("Payment {} cancelled in contract {}", paymentId, contractId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validatePaymentDate(RentalPaymentInputDTO dto, RentalContract contract) {
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
        boolean contractSignedOrFinalized = ContractStatus.SIGNED.equals(contract.getStatus())
                || ContractStatus.FINALIZED.equals(contract.getStatus());

        if (contractSignedOrFinalized && PaymentStatus.PAID.equals(payment.getStatus())) {
            throw new ValidationException(
                    "Não é possível " + action + " parcela PAGA após contrato assinado/finalizado");
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

