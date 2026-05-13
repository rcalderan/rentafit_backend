package br.com.rentafit.rental.adapter;

import br.com.rentafit.people.repository.CustomerRepository;
import br.com.rentafit.rental.port.CustomerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter que implementa CustomerPort usando CustomerRepository do componente People.
 *
 * <p>Ao migrar para microserviço, substituir esta classe por um cliente HTTP
 * que chama /api/v1/customers/{id} do serviço People, sem alterar o CustomerPort.</p>
 */
@Component
@RequiredArgsConstructor
public class CustomerAdapter implements CustomerPort {

    private final CustomerRepository customerRepository;

    @Override
    public Optional<CustomerSnapshot> findById(UUID customerId) {
        return customerRepository.findById(customerId)
                .map(c -> new CustomerSnapshot(c.getId(), c.getName(), c.getDocument()));
    }
}

