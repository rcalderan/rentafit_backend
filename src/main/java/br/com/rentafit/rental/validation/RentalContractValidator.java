package br.com.rentafit.rental.validation;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.rental.port.AccessoryPort;
import br.com.rentafit.rental.port.CustomerPort;
import br.com.rentafit.rental.port.RentalItemPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
}

