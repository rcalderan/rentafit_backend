package br.com.rentafit.rental.service;

import br.com.rentafit.product.domain.enums.ProductStatus;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.enums.ItemMetaType;
import br.com.rentafit.rental.port.AccessoryPort;
import br.com.rentafit.rental.port.RentalItemPort;
import br.com.rentafit.rental.repository.RentalContractItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RentalReservationDelta {
    private final RentalItemPort rentalItemPort;
    private final AccessoryPort accessoryPort;
    private final RentalContractItemRepository itemRepository;

    public void apply(RentalContract original, RentalContract revision) {
        Set<UUID> before = itemIds(original);
        Set<UUID> after = itemIds(revision);
        rentalItemPort.lockItems(java.util.stream.Stream.concat(before.stream(), after.stream()).distinct().sorted().toList());
        before.stream().filter(id -> !after.contains(id)).forEach(id -> releaseItem(id, original.getId()));
        after.stream().filter(id -> !before.contains(id)).forEach(this::reserveItem);
        Map<UUID, Long> previous = accessoryCounts(original);
        Map<UUID, Long> next = accessoryCounts(revision);
        java.util.stream.Stream.concat(previous.keySet().stream(), next.keySet().stream()).distinct().sorted()
                .forEach(id -> applyAccessoryDelta(id, next.getOrDefault(id, 0L) - previous.getOrDefault(id, 0L), revision.getCreatedByEmployeeId()));
    }

    public void reserveItem(UUID id) {
        rentalItemPort.findById(id).filter(item -> item.status() == ProductStatus.AVAILABLE)
                .ifPresent(item -> rentalItemPort.updateStatus(id, ProductStatus.RESERVED));
    }

    public void releaseItem(UUID id, UUID originalId) {
        if (itemRepository.countOtherReservations(id, originalId) > 0) return;
        rentalItemPort.findById(id).filter(item -> item.status() == ProductStatus.RESERVED)
                .ifPresent(item -> rentalItemPort.updateStatus(id, ProductStatus.AVAILABLE));
    }

    private void applyAccessoryDelta(UUID id, long difference, UUID actorId) {
        for (long index = 0; index < Math.abs(difference); index++) {
            if (difference > 0) accessoryPort.reserveStock(id, actorId);
            else accessoryPort.releaseStock(id, actorId);
        }
    }

    private Set<UUID> itemIds(RentalContract contract) {
        return contract.getItems().stream().map(item -> item.getRentalItemId())
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
    }

    public Map<UUID, Long> accessoryCounts(RentalContract contract) {
        return contract.getItems().stream().flatMap(item -> item.getMetadata().stream())
                .filter(meta -> meta.getType() == ItemMetaType.ACESSORIO && meta.getAccessoryId() != null)
                .collect(Collectors.groupingBy(meta -> meta.getAccessoryId(), Collectors.counting()));
    }
}
