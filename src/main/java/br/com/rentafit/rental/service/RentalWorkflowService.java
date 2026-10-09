package br.com.rentafit.rental.service;

import br.com.rentafit.product.domain.enums.ProductStatus;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
import br.com.rentafit.rental.domain.enums.ItemMetaType;
import br.com.rentafit.rental.port.AccessoryPort;
import br.com.rentafit.rental.port.RentalItemPort;
import br.com.rentafit.rental.repository.RentalContractItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orquestra os efeitos colaterais do ciclo de vida do contrato sobre o estoque e o status dos itens.
 *
 * <p>Ciclo de vida do RentalItem: AVAILABLE → RESERVED → RENTED → MAINTENANCE → AVAILABLE</p>
 * <p>Ciclo de vida do Accessory (estoque): reserve() → releaseStock()</p>
 *
 * <p>Responsabilidades por evento:
 * <ul>
 *   <li>onSign: sem efeitos colaterais no estoque.</li>
 *   <li>onFinalize: RentalItem → RESERVED; Acessório catalogado → reserveStock().</li>
 *   <li>onDeliverItem: RentalItem específico → RENTED.</li>
 *   <li>onReturn: todos os RentalItems → MAINTENANCE; Acessórios catalogados → releaseStock().</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RentalWorkflowService {

    private final RentalItemPort rentalItemPort;
    private final AccessoryPort accessoryPort;
    private final RentalContractItemRepository contractItemRepository;
    private final RentalReservationDelta reservationDelta;

    /**
     * Processado ao FINALIZAR um contrato (SIGNED → FINALIZED).
     * Reserva todos os itens e acessórios catalogados.
     */
    public void onFinalize(RentalContract contract) {
        UUID systemUserId = contract.getCreatedByEmployeeId();

        for (RentalContractItem item : contract.getItems()) {
            // Reserva o item físico
            if (item.getRentalItemId() != null) {
                reservationDelta.reserveItem(item.getRentalItemId());
                log.info("RentalItem {} RESERVED for contract {}", item.getRentalItemId(), contract.getId());
            }

        }
        // Reserva acessórios catalogados
        contract.getItems().stream().flatMap(item -> item.getMetadata().stream())
                .filter(meta -> ItemMetaType.ACESSORIO.equals(meta.getType()) && meta.getAccessoryId() != null)
                .sorted(java.util.Comparator.comparing(RentalContractItemMeta::getAccessoryId)).forEach(meta -> {
                    accessoryPort.reserveStock(meta.getAccessoryId(), systemUserId);
                    log.info("Accessory {} stock reserved for contract {}", meta.getAccessoryId(), contract.getId());
                });
    }

    /**
     * Processado ao confirmar a entrega de um item específico.
     * RentalItem → RENTED.
     */
    public void onDeliverItem(RentalContractItem item, UUID attendantEmployeeId) {
        if (item.getRentalItemId() != null) {
            rentalItemPort.lockItems(java.util.List.of(item.getRentalItemId()));
            if (contractItemRepository.countOutstandingDeliveries(item.getRentalItemId()) > 0) {
                throw new br.com.rentafit.common.exception.ValidationException("Item " + item.getRentalItemId() + ": esperado item devolvido antes de outra entrega");
            }
            var physical = rentalItemPort.findById(item.getRentalItemId()).orElseThrow(() ->
                    new br.com.rentafit.common.exception.ValidationException("Item não encontrado: " + item.getRentalItemId()));
            if (physical.status() != ProductStatus.AVAILABLE && physical.status() != ProductStatus.RESERVED) {
                throw new br.com.rentafit.common.exception.ValidationException("Item " + physical.id() + " em " + physical.status() + ": esperado AVAILABLE ou RESERVED para entrega");
            }
            rentalItemPort.updateStatus(item.getRentalItemId(), ProductStatus.RENTED);
            log.info("RentalItem {} RENTED (delivered) for contract {}", item.getRentalItemId(), item.getContract().getId());
        }
        item.setDelivered(true);
        item.setAttendantEmployeeId(attendantEmployeeId);
        contractItemRepository.save(item);
    }

    /**
     * Processado ao dar baixa no contrato (devolução).
     * Todos os RentalItems → MAINTENANCE; Acessórios catalogados → releaseStock().
     */
    public void onReturn(RentalContract contract) {
        rentalItemPort.lockItems(contract.getItems().stream().map(RentalContractItem::getRentalItemId)
                .filter(java.util.Objects::nonNull).distinct().sorted().toList());
        UUID systemUserId = contract.getReturnedByEmployeeId() != null
                ? contract.getReturnedByEmployeeId()
                : contract.getCreatedByEmployeeId();

        for (RentalContractItem item : contract.getItems()) {
            if (item.getRentalItemId() != null && contractItemRepository.countOutstandingDeliveries(item.getRentalItemId()) == 0) {
                rentalItemPort.updateStatus(item.getRentalItemId(), ProductStatus.MAINTENANCE);
                log.info("RentalItem {} → MAINTENANCE after return for contract {}", item.getRentalItemId(), contract.getId());
            }

            for (RentalContractItemMeta meta : item.getMetadata()) {
                if (ItemMetaType.ACESSORIO.equals(meta.getType()) && meta.getAccessoryId() != null) {
                    accessoryPort.releaseStock(meta.getAccessoryId(), systemUserId);
                    log.info("Accessory {} stock released after return for contract {}", meta.getAccessoryId(), contract.getId());
                }
            }
        }
    }

    /**
     * Processado na DESISTÊNCIA de um contrato que chegou a FINALIZED.
     *
     * <p>Difere de {@link #onReturn}: itens nunca entregues voltam direto a
     * AVAILABLE (não saíram da loja — não precisam de higienização); itens
     * entregues seguem para MAINTENANCE como na devolução normal.
     * Acessórios catalogados → releaseStock().</p>
     *
     * <p>Contratos SIGNED não passam por aqui — a reserva é apenas lógica
     * (ItemConflictChecker) e sai do ar com a mudança de status.</p>
     */
    public void onWithdraw(RentalContract contract) {
        rentalItemPort.lockItems(contract.getItems().stream().map(RentalContractItem::getRentalItemId)
                .filter(java.util.Objects::nonNull).distinct().sorted().toList());
        UUID systemUserId = contract.getReturnedByEmployeeId() != null
                ? contract.getReturnedByEmployeeId()
                : contract.getCreatedByEmployeeId();

        for (RentalContractItem item : contract.getItems()) {
            if (item.getRentalItemId() != null) {
                if (Boolean.TRUE.equals(item.getDelivered())) {
                    if (contractItemRepository.countOutstandingDeliveries(item.getRentalItemId()) == 0) {
                        rentalItemPort.updateStatus(item.getRentalItemId(), ProductStatus.MAINTENANCE);
                        log.info("RentalItem {} → MAINTENANCE after withdrawal for contract {}", item.getRentalItemId(), contract.getId());
                    }
                } else {
                    reservationDelta.releaseItem(item.getRentalItemId(), contract.getId());
                    log.info("RentalItem {} released after withdrawal (never delivered) for contract {}", item.getRentalItemId(), contract.getId());
                }
            }

            for (RentalContractItemMeta meta : item.getMetadata()) {
                if (ItemMetaType.ACESSORIO.equals(meta.getType()) && meta.getAccessoryId() != null) {
                    accessoryPort.releaseStock(meta.getAccessoryId(), systemUserId);
                    log.info("Accessory {} stock released after withdrawal for contract {}", meta.getAccessoryId(), contract.getId());
                }
            }
        }
    }
}

