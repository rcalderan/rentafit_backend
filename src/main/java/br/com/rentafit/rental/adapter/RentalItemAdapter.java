package br.com.rentafit.rental.adapter;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.product.domain.RentalItem;
import br.com.rentafit.product.domain.enums.ProductStatus;
import br.com.rentafit.product.repository.RentalItemRepository;
import br.com.rentafit.rental.port.RentalItemPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adapter que implementa RentalItemPort usando RentalItemRepository do componente Product.
 *
 * <p>Ao migrar para microserviço, substituir por cliente HTTP do serviço Product.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RentalItemAdapter implements RentalItemPort {

    private final RentalItemRepository rentalItemRepository;

    @Override
    public Optional<RentalItemSnapshot> findById(UUID rentalItemId) {
        return rentalItemRepository.findById(rentalItemId).map(this::toSnapshot);
    }

    @Override
    public Optional<RentalItemSnapshot> findByLegacyId(Integer legacyId) {
        return rentalItemRepository.findByLegacyId(legacyId).map(this::toSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, RentalItemSnapshot> findByIds(Collection<UUID> rentalItemIds) {
        if (rentalItemIds == null || rentalItemIds.isEmpty()) {
            return Map.of();
        }
        return rentalItemRepository.findAllById(rentalItemIds).stream()
                .map(this::toSnapshot)
                .collect(Collectors.toMap(RentalItemSnapshot::id, Function.identity()));
    }

    @Override
    @Transactional
    public void updateStatus(UUID rentalItemId, ProductStatus newStatus) {
        RentalItem item = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalItem", "id", rentalItemId.toString()));
        item.setStatus(newStatus);
        rentalItemRepository.save(item);
        log.info("RentalItem {} status updated to {}", rentalItemId, newStatus);
    }

    @Override
    public boolean isAvailable(UUID rentalItemId) {
        return rentalItemRepository.findById(rentalItemId)
                .map(item -> ProductStatus.AVAILABLE.equals(item.getStatus()))
                .orElse(false);
    }

    private RentalItemSnapshot toSnapshot(RentalItem item) {
        return new RentalItemSnapshot(
                item.getId(),
                item.getLegacyId(),
                item.getName(),
                item.getCategory() != null ? item.getCategory().getDisplayName() : null,
                item.getSize(),
                item.getColor(),
                item.getValue(),
                item.getStatus()
        );
    }
}

