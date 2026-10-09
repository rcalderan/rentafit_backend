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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 *   <li>Parcelas PAID listadas em refundPaymentIds → REFUNDED.</li>
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

        validateRefundSelection(contract, dto.refundPaymentIds());

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

    private void validateRefundSelection(RentalContract contract, List<UUID> refundPaymentIds) {
        if (refundPaymentIds == null || refundPaymentIds.isEmpty()) {
            return;
        }
        Set<UUID> paidIds = new HashSet<>(contract.getPayments().stream()
                .filter(p -> PaymentStatus.PAID.equals(p.getStatus()))
                .map(RentalPayment::getId)
                .toList());
        for (UUID id : refundPaymentIds) {
            if (!paidIds.contains(id)) {
                throw new ValidationException(
                        "Parcela " + id + " não é um pagamento PAID do contrato " + contract.getId()
                                + "; somente parcelas pagas podem ser reembolsadas");
            }
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
        Set<UUID> refundIds = dto.refundPaymentIds() == null
                ? Set.of()
                : new HashSet<>(dto.refundPaymentIds());

        for (RentalPayment payment : contract.getPayments()) {
            if (refundIds.contains(payment.getId())) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
                log.info("Payment {} refunded for contract {}", payment.getId(), contract.getId());
            } else if (PaymentStatus.PENDING.equals(payment.getStatus())) {
                payment.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(payment);
            }
        }
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
