package br.com.rentafit.migration.writer;

import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * ItemWriter que persiste entidades Employee no PostgreSQL
 *
 * Utiliza EmployeeRepository para salvar via JPA
 */
@Component
@RequiredArgsConstructor
public class EmployeeItemWriter implements ItemWriter<Employee> {

    private static final Logger log = LoggerFactory.getLogger(EmployeeItemWriter.class);

    private final EmployeeRepository employeeRepository;

    @Override
    public void write(Chunk<? extends Employee> chunk) throws Exception {
        try {
            employeeRepository.saveAll(chunk.getItems());
            log.info("Written {} employees to PostgreSQL", chunk.size());
        } catch (Exception e) {
            log.error("Error writing employees", e);
            throw e;
        }
    }
}

