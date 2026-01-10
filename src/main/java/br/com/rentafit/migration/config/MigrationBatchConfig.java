package br.com.rentafit.migration.config;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.migration.dto.ClienteDocument;
import br.com.rentafit.migration.dto.FuncionarioDocument;
import br.com.rentafit.migration.listener.MigrationJobListener;
import br.com.rentafit.migration.processor.ClienteItemProcessor;
import br.com.rentafit.migration.processor.FuncionarioItemProcessor;
import br.com.rentafit.migration.reader.ClienteItemReader;
import br.com.rentafit.migration.reader.FuncionarioItemReader;
import br.com.rentafit.migration.validator.MigrationValidator;
import br.com.rentafit.migration.writer.CustomerItemWriter;
import br.com.rentafit.migration.writer.EmployeeItemWriter;
import br.com.rentafit.migration.writer.UserAccountItemWriter;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.domain.Employee;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuração principal do Spring Batch para migração de dados
 *
 * Orquestra:
 * - Job de migração com múltiplos steps
 * - Step para cliente (read → process → write)
 * - Step para funcionário (read → process → write)
 * - Step de validação pós-migração
 */
@Configuration
@RequiredArgsConstructor
public class MigrationBatchConfig {

    private static final Logger log = LoggerFactory.getLogger(MigrationBatchConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MigrationProperties migrationProperties;
    private final ClienteItemReader clienteItemReader;
    private final FuncionarioItemReader funcionarioItemReader;
    private final ClienteItemProcessor clienteItemProcessor;
    private final FuncionarioItemProcessor funcionarioItemProcessor;
    private final CustomerItemWriter customerItemWriter;
    private final EmployeeItemWriter employeeItemWriter;
    private final UserAccountItemWriter userAccountItemWriter;
    private final UserAccountRepository userAccountRepository;
    private final MigrationValidator migrationValidator;
    private final MigrationJobListener migrationJobListener;

    /**
     * Job principal de migração
     * Inclui steps condicionais baseado em migrationProperties.entities
     */
    @Bean
    public Job migrationJob(Step clienteStep, Step funcionarioStep, Step validationStep) {
        JobBuilder jobBuilder = new JobBuilder("migrationJob", jobRepository)
            .listener(migrationJobListener)
            .incrementer(new RunIdIncrementer());

        // Adicionar steps condicionalmente
        boolean migrateCliente = migrationProperties.shouldMigrateCliente();
        boolean migrateFuncionario = migrationProperties.shouldMigrateFuncionario();

        if (migrateCliente && migrateFuncionario) {
            return jobBuilder
                .start(clienteStep)
                .next(funcionarioStep)
                .next(validationStep)
                .build();
        } else if (migrateCliente) {
            return jobBuilder
                .start(clienteStep)
                .next(validationStep)
                .build();
        } else if (migrateFuncionario) {
            return jobBuilder
                .start(funcionarioStep)
                .next(validationStep)
                .build();
        } else {
            log.warn("No entities configured for migration. Check migration.entities property");
            // JobBuilder precisa de pelo menos um step no Spring Batch 5.x
            return jobBuilder
                .start(validationStep)
                .build();
        }
    }

    /**
     * Step para migração de clientes
     * Read: ClienteItemReader (BSON) → Process: ClienteItemProcessor → Write: CustomerItemWriter
     */
    @Bean
    public Step clienteStep(
        ClienteItemReader reader,
        ClienteItemProcessor processor,
        CustomerItemWriter customerWriter) {

        log.info("Configuring Cliente Migration Step");

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(migrationProperties.getBatch().getRetryBackoffMs());

        return new StepBuilder("clienteStep", jobRepository)
            .<ClienteDocument, Customer>chunk(migrationProperties.getBatch().getChunkSize(), transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(customerWriter)
            .faultTolerant()
            .skip(Exception.class)
            .skipLimit(migrationProperties.getBatch().getSkipLimit())
            .retry(Exception.class)
            .retryLimit(migrationProperties.getBatch().getMaxRetries())
            .backOffPolicy(backOffPolicy)
            .build();
    }

    /**
     * Step para migração de funcionários
     * Read: FuncionarioItemReader (BSON) → Process: FuncionarioItemProcessor → Write: EmployeeItemWriter + UserAccountItemWriter
     */
    @Bean
    public Step funcionarioStep(
        FuncionarioItemReader reader,
        FuncionarioItemProcessor processor,
        EmployeeItemWriter employeeWriter) {

        log.info("Configuring Funcionario Migration Step");

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(migrationProperties.getBatch().getRetryBackoffMs());

        return new StepBuilder("funcionarioStep", jobRepository)
            .<FuncionarioDocument, Employee>chunk(migrationProperties.getBatch().getChunkSize(), transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(employeeWriter)
            .writer(items -> {
                // Writer adicional para UserAccount
                for (Employee item : items.getItems()) {
                    // Aqui seria necessário manter referência ao FuncionarioDocument original
                    // Para este exemplo, estamos apenas salvando Employee
                    log.debug("Written employee: {} (will be associated with UserAccount separately)", item.getName());
                }
            })
            .faultTolerant()
            .skip(Exception.class)
            .skipLimit(migrationProperties.getBatch().getSkipLimit())
            .retry(Exception.class)
            .retryLimit(migrationProperties.getBatch().getMaxRetries())
            .backOffPolicy(backOffPolicy)
            .build();
    }

    /**
     * Step de validação pós-migração
     * Executa validações e relatórios sobre integridade dos dados
     */
    @Bean
    public Step validationStep() {
        log.info("Configuring Validation Step");

        return new StepBuilder("validationStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("Starting validation step...");

                // Executar validações
                MigrationValidator.ValidationResult result = migrationValidator.validate();

                // Definir status baseado em resultado
                if (result.isSuccessful()) {
                    log.info("✓ Validation passed");
                    contribution.setExitStatus(
                        new org.springframework.batch.core.ExitStatus("VALIDATION_SUCCESS")
                    );
                    return RepeatStatus.FINISHED;
                } else {
                    log.error("✗ Validation failed with {} errors", result.getErrorCount());
                    // Decidir se falha o job ou apenas warning
                    if (migrationProperties.getSkipValidation()) {
                        log.warn("Skipping validation failure (skip.validation = true)");
                        return RepeatStatus.FINISHED;
                    } else {
                        throw new IllegalStateException("Migration validation failed");
                    }
                }
            }, transactionManager)
            .build();
    }
}

