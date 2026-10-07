package br.com.rentafit.rental.validation;

import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.dto.ContractItemInputDTO;
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
 * <p>Acionado nas transições de estado (sign e finalize) e também no
 * salvamento de propostas (create/update), para bloquear cedo qualquer
 * conflito de reserva contra contratos SIGNED/FINALIZED.</p>
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

    private final br.com.rentafit.settings.service.ApplicationSettingsService settings;

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
        UUID parentId = items.stream().map(RentalContractItem::getContract)
                .filter(java.util.Objects::nonNull).map(RentalContract::getParentContractId)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        return checkRefs(items.stream()
                        .map(i -> new RentalItemRef(i.getRentalItemId(), i.getDescription())).toList(),
                eventDate, excludeContractId, parentId);
    }

    /**
     * Checagem antes de existir entidade persistida (create/update de DRAFT/REVISION).
     * Ao editar uma revisão, o contrato-pai deve ser informado em {@code parentContractId}
     * para que as reservas vigentes do original não bloqueiem a própria revisão.
     *
     * @param items             Itens vindos do DTO de entrada
     * @param eventDate         Data do evento do contrato
     * @param excludeContractId UUID do próprio contrato (null na criação)
     * @param parentContractId  UUID do contrato original de uma revisão (null fora de revisão)
     * @return Lista de conflitos encontrados
     */
    public List<ItemConflict> checkDraft(
            List<ContractItemInputDTO> items,
            LocalDate eventDate,
            UUID excludeContractId,
            UUID parentContractId
    ) {
        if (items == null) return List.of();
        return checkRefs(items.stream()
                        .map(i -> new RentalItemRef(i.rentalItemId(), i.description())).toList(),
                eventDate, excludeContractId, parentContractId);
    }

    private List<ItemConflict> checkRefs(
            List<RentalItemRef> items,
            LocalDate eventDate,
            UUID excludeContractId,
            UUID parentContractId
    ) {
        List<ItemConflict> conflicts = new ArrayList<>();
        int windowDays = settings.rentalWindowDays();

        for (RentalItemRef item : items) {
            if (item.rentalItemId() == null) {
                continue; // item sem vínculo catalogado — ignorar
            }

            LocalDate start = eventDate.minusDays(windowDays);
            LocalDate end   = eventDate.plusDays(windowDays);

            List<RentalContractItem> candidates = contractItemRepository
                    .findConflictCandidates(item.rentalItemId(), start, end, ContractStatus.RESERVATION_STATUSES);

            for (RentalContractItem candidate : candidates) {
                UUID candidateContractId = candidate.getContract().getId();

                // Excluir o próprio contrato e o original de uma revisão
                if (candidateContractId.equals(excludeContractId) || candidateContractId.equals(parentContractId)) continue;

                LocalDate candidateEventDate = candidate.getContract().getEventDate();
                if (candidateEventDate.isBefore(start) || candidateEventDate.isAfter(end)) continue;

                conflicts.add(new ItemConflict(
                        item.rentalItemId(),
                        item.description(),
                        candidateEventDate,
                        candidateContractId,
                        ConflictSeverity.BLOCKING
                ));
            }
        }

        log.debug("Conflict check for eventDate={}: {} conflict(s) found", eventDate, conflicts.size());
        return conflicts;
    }
}

