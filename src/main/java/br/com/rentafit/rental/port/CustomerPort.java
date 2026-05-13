package br.com.rentafit.rental.port;

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
     * Snapshot imutável dos dados do cliente gravados no contrato.
     * Desacopla o contrato de alterações futuras no cadastro do cliente.
     */
    record CustomerSnapshot(UUID id, String name, String document) {}
}

