package br.com.rentafit.rental.port;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de acesso ao componente People (Customer).
 *
 * <p>Abstrai a dependência do componente Rental sobre o componente People.
 * Em um monolito, a implementação (CustomerAdapter) injeta CustomerRepository diretamente.
 * Ao migrar para microserviço, apenas o adapter é substituído por um cliente HTTP.</p>
 */
public interface CustomerPort {

    Optional<CustomerSnapshot> findById(UUID customerId);

    /**
     * Lookup em lote de legacyId dos clientes (Person.legacyId, nullable).
     * Usado em listagens para evitar N+1 — ausência no mapa ou valor null
     * significa cliente sem legacyId.
     */
    Map<UUID, Integer> findLegacyIdsByIds(Collection<UUID> customerIds);

    /**
     * Snapshot imutável dos dados do cliente gravados no contrato.
     * Desacopla o contrato de alterações futuras no cadastro do cliente.
     */
    record CustomerSnapshot(UUID id, String name, String document) {}
}

