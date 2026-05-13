package br.com.rentafit.sales.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de acesso ao componente People (Customer) para o módulo Sales.
 * Reutiliza o mesmo snapshot pattern de CustomerPort do módulo Rental.
 */
public interface SalesCustomerPort {

    Optional<CustomerSnapshot> findById(UUID customerId);

    record CustomerSnapshot(UUID id, String name, String document) {}
}
