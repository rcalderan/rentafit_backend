package br.com.rentafit.migration.writer;

import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * ItemWriter que persiste entidades Customer no PostgreSQL
 *
 * Utiliza CustomerRepository para salvar via JPA
 */
@Component
@RequiredArgsConstructor
public class CustomerItemWriter implements ItemWriter<Customer> {

    private static final Logger log = LoggerFactory.getLogger(CustomerItemWriter.class);

    private final CustomerRepository customerRepository;

    @Override
    public void write(Chunk<? extends Customer> chunk) throws Exception {
        try {
            customerRepository.saveAll(chunk.getItems());
            log.info("Written {} customers to PostgreSQL", chunk.size());
        } catch (Exception e) {
            log.error("Error writing customers", e);
            throw e;
        }
    }
}

