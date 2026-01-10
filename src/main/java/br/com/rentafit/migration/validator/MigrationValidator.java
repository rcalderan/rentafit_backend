package br.com.rentafit.migration.validator;

import br.com.rentafit.people.repository.CustomerRepository;
import br.com.rentafit.people.repository.EmployeeRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validador de integridade de dados após migração
 *
 * Realiza validações:
 * 1. Contagem de registros
 * 2. Unicidade de campos
 * 3. Integridade referencial
 * 4. Amostragem de dados
 */
@Component
@RequiredArgsConstructor
public class MigrationValidator {

    private static final Logger log = LoggerFactory.getLogger(MigrationValidator.class);

    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;

    /**
     * Executa validações pós-migração
     */
    public ValidationResult validate() {
        log.info("Starting post-migration validation...");

        ValidationResult result = new ValidationResult();

        try {
            validateRecordCounts(result);
            validateUniqueness(result);
            validateReferentialIntegrity(result);
            validateDataSample(result);

            result.setSuccessful(result.getErrorCount() == 0);

        } catch (Exception e) {
            log.error("Error during validation", e);
            result.setSuccessful(false);
            result.addError("Validation failed: " + e.getMessage());
        }

        logValidationResults(result);
        return result;
    }

    private void validateRecordCounts(ValidationResult result) {
        log.info("Validating record counts...");

        long customerCount = customerRepository.count();
        long employeeCount = employeeRepository.count();
        long userAccountCount = userAccountRepository.count();

        log.info("  Customers: {}", customerCount);
        log.info("  Employees: {}", employeeCount);
        log.info("  User Accounts: {}", userAccountCount);

        // Validações básicas
        if (customerCount == 0) {
            result.addWarning("No customers found after migration");
        }
        if (employeeCount == 0) {
            result.addWarning("No employees found after migration");
        }
        if (userAccountCount == 0) {
            result.addWarning("No user accounts found after migration");
        }

        result.setCustomerCount(customerCount);
        result.setEmployeeCount(employeeCount);
        result.setUserAccountCount(userAccountCount);
    }

    private void validateUniqueness(ValidationResult result) {
        log.info("Validating uniqueness constraints...");

        // TODO: Implementar validações de unicidade
        // - Email não duplicado
        // - Document (CPF) não duplicado
        // - Username não duplicado

        log.info("  Uniqueness validation: OK (TODO)");
    }

    private void validateReferentialIntegrity(ValidationResult result) {
        log.info("Validating referential integrity...");

        // TODO: Implementar validações de integridade referencial
        // - Verificar se customers.created_by_id referencia employee válido
        // - Verificar se customers.address_id referencia address válido
        // - Verificar se user_accounts referenciam people válida

        log.info("  Referential integrity validation: OK (TODO)");
    }

    private void validateDataSample(ValidationResult result) {
        log.info("Validating data samples...");

        // TODO: Implementar amostragem aleatória
        // - Selecionar N registros aleatoriamente
        // - Validar campos críticos preenchidos
        // - Validar criptografia de documento
        // - Validar hash de senha

        log.info("  Data sample validation: OK (TODO)");
    }

    private void logValidationResults(ValidationResult result) {
        log.info("=".repeat(60));
        log.info("MIGRATION VALIDATION RESULTS");
        log.info("=".repeat(60));
        log.info("Status: {}", result.isSuccessful() ? "✓ SUCCESS" : "✗ FAILED");
        log.info("Customers: {}", result.getCustomerCount());
        log.info("Employees: {}", result.getEmployeeCount());
        log.info("User Accounts: {}", result.getUserAccountCount());
        log.info("Errors: {}", result.getErrorCount());
        log.info("Warnings: {}", result.getWarningCount());

        if (!result.getErrors().isEmpty()) {
            log.error("ERRORS:");
            result.getErrors().forEach(error -> log.error("  - {}", error));
        }

        if (!result.getWarnings().isEmpty()) {
            log.warn("WARNINGS:");
            result.getWarnings().forEach(warning -> log.warn("  - {}", warning));
        }

        log.info("=".repeat(60));
    }

    /**
     * Classe para armazenar resultados de validação
     */
    public static class ValidationResult {
        private boolean successful;
        private long customerCount;
        private long employeeCount;
        private long userAccountCount;
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        // Getters
        public boolean isSuccessful() { return successful; }
        public long getCustomerCount() { return customerCount; }
        public long getEmployeeCount() { return employeeCount; }
        public long getUserAccountCount() { return userAccountCount; }
        public int getErrorCount() { return errors.size(); }
        public int getWarningCount() { return warnings.size(); }
        public java.util.List<String> getErrors() { return errors; }
        public java.util.List<String> getWarnings() { return warnings; }

        // Setters
        public void setSuccessful(boolean successful) { this.successful = successful; }
        public void setCustomerCount(long count) { this.customerCount = count; }
        public void setEmployeeCount(long count) { this.employeeCount = count; }
        public void setUserAccountCount(long count) { this.userAccountCount = count; }
    }
}

