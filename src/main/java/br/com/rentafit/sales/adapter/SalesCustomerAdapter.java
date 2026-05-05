package br.com.rentafit.sales.adapter;

import br.com.rentafit.people.repository.CustomerRepository;
import br.com.rentafit.sales.port.SalesCustomerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter que implementa SalesCustomerPort usando CustomerRepository do componente People.
 */
@Component
@RequiredArgsConstructor
public class SalesCustomerAdapter implements SalesCustomerPort {

    private final CustomerRepository customerRepository;

    @Override
    public Optional<CustomerSnapshot> findById(UUID customerId) {
        return customerRepository.findById(customerId)
                .map(c -> new CustomerSnapshot(c.getId(), c.getName(), c.getDocument()));
    }
}
