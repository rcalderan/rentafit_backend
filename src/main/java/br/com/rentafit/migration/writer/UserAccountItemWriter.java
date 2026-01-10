package br.com.rentafit.migration.writer;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * ItemWriter que persiste entidades UserAccount no PostgreSQL
 *
 * Utiliza UserAccountRepository para salvar via JPA
 */
@Component
@RequiredArgsConstructor
public class UserAccountItemWriter implements ItemWriter<UserAccount> {

    private static final Logger log = LoggerFactory.getLogger(UserAccountItemWriter.class);

    private final UserAccountRepository userAccountRepository;

    @Override
    public void write(Chunk<? extends UserAccount> chunk) throws Exception {
        try {
            userAccountRepository.saveAll(chunk.getItems());
            log.info("Written {} user accounts to PostgreSQL", chunk.size());
        } catch (Exception e) {
            log.error("Error writing user accounts", e);
            throw e;
        }
    }
}

