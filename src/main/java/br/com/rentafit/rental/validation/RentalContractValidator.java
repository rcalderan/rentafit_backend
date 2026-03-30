package br.com.rentafit.rental.validation;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.ContractItemInputDTO;
import br.com.rentafit.rental.dto.RentalPaymentInputDTO;
import br.com.rentafit.rental.port.AccessoryPort;
import br.com.rentafit.rental.port.CustomerPort;
import br.com.rentafit.rental.port.RentalItemPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orquestrador de validações de negócio do contrato de locação.
 *
 * <p>Validações básicas (create/update): ordem de datas, customer, items, acessórios.</p>
 * <p>Validações de transição (sign/finalize): básicas + checagem de conflitos via ItemConflictChecker.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RentalContractValidator {

    private final CustomerPort customerPort;
    private final RentalItemPort rentalItemPort;
    private final AccessoryPort accessoryPort;
    private final ItemConflictChecker conflictChecker;

    // ── Validações básicas (sempre ativas) ────────────────────────────────────

    /**
     * Valida a ordem das datas: pickupDate ≤ eventDate ≤ returnDate.
     */
    public void validateDateOrder(LocalDate pickupDate, LocalDate eventDate, LocalDate returnDate) {
        List<String> errors = new ArrayList<>();

        if (pickupDate != null && eventDate != null && pickupDate.isAfter(eventDate)) {
            errors.add("Data de retirada (" + pickupDate + ") não pode ser posterior à data de uso (" + eventDate + ")");
        }
        if (eventDate != null && returnDate != null && eventDate.isAfter(returnDate)) {
            errors.add("Data de uso (" + eventDate + ") não pode ser posterior à data de devolução (" + returnDate + ")");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }

    /**
     * Valida que o cliente existe via CustomerPort.
     */
    public CustomerPort.CustomerSnapshot validateAndGetCustomer(UUID customerId) {
        return customerPort.findById(customerId)
                .orElseThrow(() -> new ValidationException("Cliente não encontrado: " + customerId));
    }

    /**
     * Valida que os rentalItemIds referenciados existem e estão disponíveis.
     * Itens com rentalItemId null são ignorados (itens legados sem correspondência).
     */
    public void validateItemsAvailability(List<UUID> rentalItemIds) {
        List<String> errors = new ArrayList<>();
        for (UUID itemId : rentalItemIds) {
            if (itemId == null) continue;
            if (!rentalItemPort.isAvailable(itemId)) {
                rentalItemPort.findById(itemId).ifPresentOrElse(
                        snap -> errors.add("Item '" + snap.name() + "' não está disponível (status: " + snap.status() + ")"),
                        () -> errors.add("Item não encontrado: " + itemId)
                );
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }

    /**
     * Valida que os acessórios catalogados (accessoryId != null) existem e têm estoque.
     */
    public void validateAccessoriesAvailability(List<UUID> accessoryIds) {
        List<String> errors = new ArrayList<>();
        for (UUID accessoryId : accessoryIds) {
            if (accessoryId == null) continue;
            if (!accessoryPort.isAvailableInStock(accessoryId)) {
                errors.add("Acessório sem estoque disponível: " + accessoryId);
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }

    // ── Validações de transição de estado ─────────────────────────────────────

    /**
     * Executa a checagem de conflitos e separa BLOCKING de WARNING.
     * BLOCKING → lança ValidationException.
     * WARNING → retornado como lista de mensagens para o DTO de resposta.
     *
     * @param contractItems     Itens do contrato
     * @param eventDate         Data do evento
     * @param excludeContractId UUID do próprio contrato
     * @return Lista de mensagens de alerta (WARNING)
     */
    public List<String> checkConflictsForTransition(
            List<br.com.rentafit.rental.domain.RentalContractItem> contractItems,
            LocalDate eventDate,
            UUID excludeContractId
    ) {
        List<ItemConflict> conflicts = conflictChecker.check(contractItems, eventDate, excludeContractId);

        List<String> blockingMessages = new ArrayList<>();
        List<String> warningMessages  = new ArrayList<>();

        for (ItemConflict conflict : conflicts) {
            if (conflict.severity() == ConflictSeverity.BLOCKING) {
                blockingMessages.add(conflict.toMessage());
            } else {
                warningMessages.add(conflict.toMessage());
            }
        }

        if (!blockingMessages.isEmpty()) {
            throw new ValidationException("Conflito de reserva: " + String.join("; ", blockingMessages));
        }

        return warningMessages.isEmpty() ? null : warningMessages;
    }

    // ── Validações de item ────────────────────────────────────────────────────

    /**
     * Valida que todo item possui {@code customerId} informado.
     * Deve ser chamado antes de persistir itens (create/update de contrato).
     *
     * @param items lista de itens a validar (null/vazia é ignorada)
     * @throws ValidationException se algum item não tiver cliente associado
     */
    public void validateItemsHaveAttendant(List<ContractItemInputDTO> items) {
        if (items == null || items.isEmpty()) return;

        List<String> errors = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ContractItemInputDTO item = items.get(i);
            if (item.attendantEmployeeId() == null) {
                String label = item.description() != null
                        ? "\"" + item.description() + "\""
                        : "(índice " + (i + 1) + ")";
                errors.add("Item " + label + ": O atendente é obrigatório para cada item do contrato");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }

    // ── Validações de pagamento ───────────────────────────────────────────────

    /**
     * Valida que toda parcela com status PAID possui {@code processedByEmployeeId} informado.
     * Deve ser chamado antes de persistir parcelas (create/update de contrato e add/update de parcela isolada).
     *
     * @param payments lista de parcelas a validar (null/vazia é ignorada)
     * @throws ValidationException se alguma parcela PAID não tiver funcionário responsável
     */
    public void validatePaidPaymentsHaveEmployee(List<RentalPaymentInputDTO> payments) {
        if (payments == null || payments.isEmpty()) return;

        List<String> errors = new ArrayList<>();
        for (int i = 0; i < payments.size(); i++) {
            RentalPaymentInputDTO p = payments.get(i);
            if (isPaid(p.status()) && p.processedByEmployeeId() == null) {
                errors.add("Parcela " + (p.installmentNumber() != null ? "#" + p.installmentNumber() : "(índice " + (i + 1) + ")")
                        + ": pagamentos com status PAGO devem informar o funcionário responsável (processedByEmployeeId)");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }

    /**
     * Valida parcelas já persistidas no contrato: toda PAID deve ter funcionário responsável.
     * Usado em transições de estado (ex.: sign) para proteger dados legados/inconsistentes.
     */
    public void validatePersistedPaidPaymentsHaveEmployee(List<RentalPayment> payments) {
        if (payments == null || payments.isEmpty()) return;

        List<String> errors = new ArrayList<>();
        for (RentalPayment p : payments) {
            if (PaymentStatus.PAID.equals(p.getStatus()) && p.getProcessedByEmployeeId() == null) {
                errors.add("Parcela " + (p.getInstallmentNumber() != null ? "#" + p.getInstallmentNumber() : "")
                        + ": pagamentos com status PAGO devem informar o funcionário responsável (processedByEmployeeId)");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }

    /**
     * Valida uma única parcela: se status for PAID, {@code processedByEmployeeId} é obrigatório.
     * Conveniente para validação em add/update de parcela individual.
     *
     * @param payment parcela a validar
     * @throws ValidationException se a parcela for PAID sem funcionário informado
     */
    public void validateSinglePaidPaymentHasEmployee(RentalPaymentInputDTO payment) {
        if (payment == null) return;
        if (isPaid(payment.status()) && payment.processedByEmployeeId() == null) {
            throw new ValidationException(
                    "Parcela" + (payment.installmentNumber() != null ? " #" + payment.installmentNumber() : "")
                            + ": pagamentos com status PAGO devem informar o funcionário responsável (processedByEmployeeId)");
        }
    }

    // ── helpers privados ──────────────────────────────────────────────────────

    /**
     * Valida que parcelas PAID existentes não foram removidas nem alteradas na revisão.
     * <p>Parcelas PENDING/CANCELLED podem ser livremente modificadas.</p>
     *
     * @param incoming lista de parcelas vindas do DTO de update
     * @param existing lista de parcelas já persistidas no contrato em REVISION
     */
    public void validateRevisionPaymentIntegrity(List<RentalPaymentInputDTO> incoming, List<RentalPayment> existing) {
        List<RentalPayment> paidPayments = existing.stream()
                .filter(p -> PaymentStatus.PAID.equals(p.getStatus()))
                .toList();

        if (paidPayments.isEmpty()) return;

        List<String> errors = new ArrayList<>();
        for (RentalPayment paid : paidPayments) {
            boolean found = incoming.stream().anyMatch(dto ->
                    paid.getInstallmentNumber().equals(dto.installmentNumber())
                    && paid.getValue().compareTo(dto.value()) == 0
                    && paid.getPaymentMethod().name().equalsIgnoreCase(dto.paymentMethod())
                    && "PAID".equalsIgnoreCase(dto.status())
            );
            if (!found) {
                errors.add("Parcela PAGA #" + paid.getInstallmentNumber()
                        + " (R$ " + paid.getValue() + ") não pode ser removida ou alterada em uma revisão");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }

    private boolean isPaid(String status) {
        if (status == null) return false;
        try {
            return PaymentStatus.PAID.equals(PaymentStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Valida que as parcelas informadas somam exatamente o valor total dos itens.
     * Parcelas com status CANCELLED são ignoradas na soma.
     *
     * @param payments Parcelas informadas no DTO (nunca null/vazio — já garantido pela anotação @NotEmpty)
     * @param items    Itens do contrato informados no DTO
     */
    public void validatePaymentsMatchTotal(List<RentalPaymentInputDTO> payments, List<ContractItemInputDTO> items) {
        BigDecimal totalItems = items == null ? BigDecimal.ZERO : items.stream()
                .map(i -> i.value() != null ? i.value() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPayments = payments.stream()
                .filter(p -> {
                    if (p.status() == null) return true; // default PENDING → conta
                    try {
                        return !PaymentStatus.CANCELLED.equals(PaymentStatus.valueOf(p.status().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        return true;
                    }
                })
                .map(p -> p.value() != null ? p.value() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPayments.compareTo(totalItems) != 0) {
            throw new ValidationException(
                    "A soma das parcelas (" + totalPayments
                            + ") deve ser igual ao valor total do contrato (" + totalItems + ")");
        }
    }
}

