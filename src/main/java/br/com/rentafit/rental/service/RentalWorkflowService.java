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

    /**
     * Processado ao FINALIZAR um contrato (SIGNED → FINALIZED).
     * Reserva todos os itens e acessórios catalogados.
     */
    public void onFinalize(RentalContract contract) {
        UUID systemUserId = contract.getCreatedByEmployeeId();

        for (RentalContractItem item : contract.getItems()) {
            // Reserva o item físico
            if (item.getRentalItemId() != null) {
                rentalItemPort.updateStatus(item.getRentalItemId(), ProductStatus.RESERVED);
                log.info("RentalItem {} RESERVED for contract {}", item.getRentalItemId(), contract.getId());
            }

            // Reserva acessórios catalogados
            for (RentalContractItemMeta meta : item.getMetadata()) {
                if (ItemMetaType.ACESSORIO.equals(meta.getType()) && meta.getAccessoryId() != null) {
                    accessoryPort.reserveStock(meta.getAccessoryId(), systemUserId);
                    log.info("Accessory {} stock reserved for contract {}", meta.getAccessoryId(), contract.getId());
                }
            }
        }
    }

    /**
     * Processado ao confirmar a entrega de um item específico.
     * RentalItem → RENTED.
     */
    public void onDeliverItem(RentalContractItem item, UUID attendantEmployeeId) {
        if (item.getRentalItemId() != null) {
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
        UUID systemUserId = contract.getReturnedByEmployeeId() != null
                ? contract.getReturnedByEmployeeId()
                : contract.getCreatedByEmployeeId();

        for (RentalContractItem item : contract.getItems()) {
            if (item.getRentalItemId() != null) {
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
}

