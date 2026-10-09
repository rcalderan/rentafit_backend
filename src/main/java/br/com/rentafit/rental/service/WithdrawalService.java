package br.com.rentafit.rental.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.repository.EmployeeRepository;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.RentalContractDetailsDTO;
import br.com.rentafit.rental.dto.WithdrawContractDTO;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.repository.RentalPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Desistência de contrato de locação: SIGNED|FINALIZED → CANCELLED.
 *
 * <p>Acionado pela tela de devolução quando a baixa ocorre antes da data
 * prevista (ou o cliente desiste da locação). O frontend imprime o Termo de
 * Desistência (template RENTAL_CANCELLATION) e só chama este endpoint após o
 * operador confirmar a assinatura do cliente.</p>
 *
 * <p>Efeitos atômicos:
 * <ul>
 *   <li>Reembolso opcional (refundAmount) → lançamento REFUNDED de valor
 *   negativo, preservando o histórico das parcelas PAID recebidas.</li>
 *   <li>Parcelas PENDING → CANCELLED.</li>
 *   <li>Multa rescisória opcional → nova parcela MULTA (registro de retenção).</li>
 *   <li>Itens/acessórios marcados como devolvidos; estoque liberado via
 *   {@link RentalWorkflowService#onWithdraw}.</li>
 *   <li>Contrato → CANCELLED (sai de RESERVATION_STATUSES, liberando as datas).</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WithdrawalService {

    private final RentalContractRepository contractRepository;
    private final RentalPaymentRepository paymentRepository;
    private final EmployeeRepository employeeRepository;
    private final RentalWorkflowService workflowService;
    private final RentalMapper mapper;

    public RentalContractDetailsDTO withdraw(UUID contractId, WithdrawContractDTO dto) {
        contractRepository.lockById(contractId);
        RentalContract contract = requireContract(contractId);
        requireWithdrawable(contract);
        boolean wasFinalized = ContractStatus.FINALIZED.equals(contract.getStatus());

        Employee employee = employeeRepository.findById(dto.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", dto.employeeId().toString()));

        validateRefundAmount(contract, dto.refundAmount());

        OffsetDateTime now = OffsetDateTime.now();
        String returnerName = "Desistência — " + employee.getName();
        markAllReturned(contract, now, returnerName);
        settlePayments(contract, dto);

        if (Boolean.TRUE.equals(dto.applyFine()) && dto.fineAmount() != null
                && dto.fineAmount().compareTo(BigDecimal.ZERO) > 0) {
            createFinePayment(contract, dto.fineAmount(), dto.employeeId());
        }

        contract.setStatus(ContractStatus.CANCELLED);
        contract.setReturned(true);
        contract.setActualReturnDate(LocalDate.now());
        contract.setReturnedByEmployeeId(dto.employeeId());
        RentalContract saved = contractRepository.save(contract);

        // SIGNED nunca reservou estoque/itens — só FINALIZED tem efeitos físicos.
        if (wasFinalized) {
            workflowService.onWithdraw(saved);
        }
        log.info("Contract {} withdrawn (desistência) by employee {}", contractId, dto.employeeId());
        return mapper.toDetailsDTO(saved, null);
    }

    // ── Helpers de negócio ────────────────────────────────────────────────────

    private void requireWithdrawable(RentalContract contract) {
        if (!ContractStatus.SIGNED.equals(contract.getStatus())
                && !ContractStatus.FINALIZED.equals(contract.getStatus())) {
            throw new ValidationException(
                    "Desistência disponível apenas para contratos SIGNED ou FINALIZED. Status atual: "
                            + contract.getStatus());
        }
    }

    private void validateRefundAmount(RentalContract contract, BigDecimal refundAmount) {
        if (refundAmount == null) {
            return;
        }
        BigDecimal paidTotal = contract.getPayments().stream()
                .filter(p -> PaymentStatus.PAID.equals(p.getStatus()))
                .map(RentalPayment::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (refundAmount.compareTo(paidTotal) > 0) {
            throw new ValidationException(
                    "refundAmount " + refundAmount + " excede o total pago do contrato "
                            + contract.getId() + " (" + paidTotal + ")");
        }
    }

    private void markAllReturned(RentalContract contract, OffsetDateTime now, String returnerName) {
        for (RentalContractItem item : contract.getItems()) {
            if (!Boolean.TRUE.equals(item.getReturned())) {
                item.setReturned(true);
                item.setReturnedAt(now);
                item.setReturnedByName(returnerName);
            }
            for (RentalContractItemMeta meta : item.getMetadata()) {
                if (!Boolean.TRUE.equals(meta.getReturned())) {
                    meta.setReturned(true);
                    meta.setReturnedAt(now);
                }
            }
        }
    }

    private void settlePayments(RentalContract contract, WithdrawContractDTO dto) {
        for (RentalPayment payment : contract.getPayments()) {
            if (PaymentStatus.PENDING.equals(payment.getStatus())) {
                payment.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(payment);
            }
        }
        if (dto.refundAmount() != null) {
            createRefundEntry(contract, dto.refundAmount(), dto.employeeId());
        }
    }

    /**
     * Lançamento REFUNDED com o valor devolvido: registra a devolução ao
     * cliente sem apagar o histórico das parcelas PAID recebidas.
     * chk_rental_payment_value exige value > 0 — o status REFUNDED é o
     * marcador de saída, não o sinal do valor.
     */
    private void createRefundEntry(RentalContract contract, BigDecimal refundAmount, UUID employeeId) {
        int nextInstallment = paymentRepository
                .findMaxInstallmentNumberByContractId(contract.getId()) + 1;

        RentalPayment refund = RentalPayment.builder()
                .contract(contract)
                .installmentNumber(nextInstallment)
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.CASH)
                .value(refundAmount)
                .installments(1)
                .status(PaymentStatus.REFUNDED)
                .processedByEmployeeId(employeeId)
                .build();

        paymentRepository.save(refund);
        log.info("Refund of {} recorded for contract {}", refundAmount, contract.getId());
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
        log.info("Rescission fine of {} created for withdrawn contract {}", fineAmount, contract.getId());
    }

    private RentalContract requireContract(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalContract", "id", contractId.toString()));
    }
}
