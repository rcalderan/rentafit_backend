package br.com.rentafit.rental.validation;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa um conflito de reserva detectado para um item de locação.
 *
 * @param rentalItemId             UUID do item em conflito
 * @param itemDescription          Descrição do item (para mensagem legível)
 * @param conflictingEventDate     Data do evento conflitante
 * @param conflictingContractId    UUID do contrato conflitante
 * @param severity                 BLOCKING (mesmo dia) ou WARNING (até 3 dias)
 */
public record ItemConflict(
        UUID rentalItemId,
        String itemDescription,
        LocalDate conflictingEventDate,
        UUID conflictingContractId,
        ConflictSeverity severity
) {
    public String toMessage() {
        return String.format(
                "Item '%s': conflito de reserva com contrato %s (evento em %s) — %s",
                itemDescription,
                conflictingContractId,
                conflictingEventDate,
                severity == ConflictSeverity.BLOCKING ? "BLOQUEIO — mesma data" : "ALERTA — dentro de 3 dias"
        );
    }
}

