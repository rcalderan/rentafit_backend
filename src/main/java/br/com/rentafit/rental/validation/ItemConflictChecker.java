package br.com.rentafit.rental.validation;

import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.repository.RentalContractItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Verifica conflitos de reserva para itens de locação.
 *
 * <p>Acionado exclusivamente nas transições de estado (sign e finalize),
 * nunca durante criação/edição de draft.</p>
 *
 * <p>Regras:
 * <ul>
 *   <li>BLOCKING: existe contrato SIGNED ou FINALIZED com o mesmo item e mesmo eventDate.</li>
 *   <li>WARNING: existe contrato SIGNED ou FINALIZED com o mesmo item com eventDate dentro de 3 dias
 *   (mas não o mesmo dia).</li>
 *   <li>Itens com rentalItemId null são ignorados.</li>
 *   <li>O próprio contrato sendo processado é excluído da busca.</li>
 * </ul>
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItemConflictChecker {

    private static final int CONFLICT_WINDOW_DAYS = 3;
    private static final List<ContractStatus> ACTIVE_STATUSES = List.of(
            ContractStatus.SIGNED, ContractStatus.FINALIZED
    );

    private final RentalContractItemRepository contractItemRepository;

    /**
     * Verifica conflitos para todos os itens de um contrato em processo de transição.
     *
     * @param items             Itens do contrato sendo processado
     * @param eventDate         Data do evento do contrato
     * @param excludeContractId UUID do próprio contrato (excluído da busca)
     * @return Lista de conflitos encontrados (pode conter BLOCKING e/ou WARNING)
     */
    public List<ItemConflict> check(
            List<RentalContractItem> items,
            LocalDate eventDate,
            UUID excludeContractId
    ) {
        List<ItemConflict> conflicts = new ArrayList<>();

        for (RentalContractItem item : items) {
            if (item.getRentalItemId() == null) {
                continue; // item sem vínculo catalogado — ignorar
            }

            LocalDate start = eventDate.minusDays(CONFLICT_WINDOW_DAYS);
            LocalDate end   = eventDate.plusDays(CONFLICT_WINDOW_DAYS);

            List<RentalContractItem> candidates = contractItemRepository
                    .findConflictCandidates(item.getRentalItemId(), start, end, ACTIVE_STATUSES);

            for (RentalContractItem candidate : candidates) {
                UUID candidateContractId = candidate.getContract().getId();

                // Excluir o próprio contrato
                if (candidateContractId.equals(excludeContractId)) continue;

                LocalDate candidateEventDate = candidate.getContract().getEventDate();
                ConflictSeverity severity = candidateEventDate.isEqual(eventDate)
                        ? ConflictSeverity.BLOCKING
                        : ConflictSeverity.WARNING;

                conflicts.add(new ItemConflict(
                        item.getRentalItemId(),
                        item.getDescription(),
                        candidateEventDate,
                        candidateContractId,
                        severity
                ));
            }
        }

        log.debug("Conflict check for eventDate={}: {} conflict(s) found", eventDate, conflicts.size());
        return conflicts;
    }
}

