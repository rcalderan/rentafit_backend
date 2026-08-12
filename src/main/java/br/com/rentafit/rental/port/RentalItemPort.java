package br.com.rentafit.rental.port;

import br.com.rentafit.product.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de acesso ao componente Product (RentalItem).
 *
 * <p>Abstrai operações de leitura e mudança de status do item de locação.
 * Ao migrar para microserviço, apenas RentalItemAdapter é substituído.</p>
 */
public interface RentalItemPort {

    Optional<RentalItemSnapshot> findById(UUID rentalItemId);

    Optional<RentalItemSnapshot> findByLegacyId(String legacyId);

    /**
     * Resolve múltiplos snapshots de uma vez (batch) para evitar N+1.
     * IDs não encontrados são simplesmente omitidos do mapa retornado.
     */
    Map<UUID, RentalItemSnapshot> findByIds(Collection<UUID> rentalItemIds);

    /**
     * Atualiza o ProductStatus do RentalItem.
     * Ciclo esperado: AVAILABLE → RESERVED → RENTED → MAINTENANCE → AVAILABLE
     */
    void updateStatus(UUID rentalItemId, ProductStatus newStatus);

    boolean isAvailable(UUID rentalItemId);

    /**
     * Snapshot dos dados do item gravado no contrato no momento da criação.
     */
    record RentalItemSnapshot(
            UUID id,
            String legacyId,
            String name,
            String categoryName,
            String size,
            String color,
            BigDecimal value,
            ProductStatus status
    ) {}
}

