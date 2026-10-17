package br.com.rentafit.rental.service;

import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.dto.ItemReservationDTO;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.port.CustomerPort;
import br.com.rentafit.rental.repository.RentalContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Consulta de reservas ativas por item de locação.
 *
 * <p>Reserva ativa = contrato em {@link ContractStatus#RESERVATION_STATUSES}
 * (SIGNED/FINALIZED, mesma regra do ItemConflictChecker) com eventDate a partir
 * de hoje — o que interessa ao operador ao carregar um item já reservado.
 * Histórico (CLOSED, SUPERSEDED, eventos passados) é intencionalmente excluído.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItemReservationService {

    private final RentalContractRepository contractRepository;
    private final CustomerPort customerPort;
    private final RentalMapper mapper;

    /**
     * Lista as reservas ativas de um item, ordenadas pelo evento mais próximo.
     *
     * <p>Com {@code includePendingReturn=true}, devoluções pendentes (FINALIZED
     * não devolvido com evento passado) entram primeiro na lista, marcadas com
     * {@code pendingReturn=true} — o modal do dashboard precisa delas para
     * responder "onde está a roupa", não só "quem a reservou".</p>
     *
     * @param rentalItemId         UUID do RentalItem no catálogo
     * @param excludeContractId    contrato a excluir da lista — o contrato em edição
     *                             não deve aparecer como reserva de si mesmo (nullable)
     * @param includePendingReturn inclui contratos FINALIZED ainda não devolvidos
     * @return reservas encontradas (lista vazia se nenhuma)
     */
    public List<ItemReservationDTO> findReservationsByItem(
            UUID rentalItemId, UUID excludeContractId, boolean includePendingReturn) {
        LocalDate today = LocalDate.now();

        List<RentalContract> pendingReturns = includePendingReturn
                ? contractRepository.findPendingReturnsByRentalItemId(rentalItemId, ContractStatus.FINALIZED, today)
                : List.of();
        List<RentalContract> upcoming = contractRepository.findReservationsByRentalItemId(
                rentalItemId, ContractStatus.RESERVATION_STATUSES, today);

        Set<UUID> pendingIds = pendingReturns.stream()
                .map(RentalContract::getId)
                .collect(Collectors.toSet());

        List<RentalContract> contracts = Stream.concat(pendingReturns.stream(), upcoming.stream())
                .filter(c -> !c.getId().equals(excludeContractId))
                .toList();

        if (contracts.isEmpty()) return List.of();

        Map<UUID, Integer> customerLegacyIds = customerPort.findLegacyIdsByIds(
                contracts.stream().map(RentalContract::getCustomerId).collect(Collectors.toSet()));

        List<ItemReservationDTO> reservations = contracts.stream()
                .map(c -> mapper.toItemReservationDTO(
                        c, customerLegacyIds.get(c.getCustomerId()), pendingIds.contains(c.getId())))
                .toList();

        log.debug("Item {} possui {} reserva(s) ativa(s) e {} devolução(ões) pendente(s)",
                rentalItemId, reservations.size() - pendingIds.size(), pendingIds.size());
        return reservations;
    }
}
