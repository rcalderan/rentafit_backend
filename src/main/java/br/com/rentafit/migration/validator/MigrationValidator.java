package br.com.rentafit.migration.validator;

import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.people.repository.CustomerRepository;
import br.com.rentafit.people.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validador de integridade de dados após migração.
 */
@Component
@RequiredArgsConstructor
public class MigrationValidator {

    private static final Logger log = LoggerFactory.getLogger(MigrationValidator.class);

    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final JdbcTemplate jdbcTemplate;

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
        long customerCount = customerRepository.count();
        long employeeCount = employeeRepository.count();
        long userAccountCount = userAccountRepository.count();

        result.setCustomerCount(customerCount);
        result.setEmployeeCount(employeeCount);
        result.setUserAccountCount(userAccountCount);

        if (customerCount == 0) {
            result.addWarning("No customers found after migration");
        }
        if (employeeCount == 0) {
            result.addWarning("No employees found after migration");
        }
        if (userAccountCount == 0) {
            result.addWarning("No user accounts found after migration");
        }
    }

    private void validateUniqueness(ValidationResult result) {
        Long duplicateEmails = queryLong("SELECT COUNT(*) FROM (SELECT email FROM people WHERE email IS NOT NULL GROUP BY email HAVING COUNT(*) > 1) t");
        Long duplicateDocuments = queryLong("SELECT COUNT(*) FROM (SELECT document FROM people WHERE document IS NOT NULL GROUP BY document HAVING COUNT(*) > 1) t");
        Long duplicateUsernames = queryLong("SELECT COUNT(*) FROM (SELECT username FROM user_accounts GROUP BY username HAVING COUNT(*) > 1) t");

        if (duplicateEmails != null && duplicateEmails > 0) {
            result.addError("Duplicate emails found: " + duplicateEmails);
        }
        if (duplicateDocuments != null && duplicateDocuments > 0) {
            result.addError("Duplicate documents found: " + duplicateDocuments);
        }
        if (duplicateUsernames != null && duplicateUsernames > 0) {
            result.addError("Duplicate usernames found: " + duplicateUsernames);
        }
    }

    private void validateReferentialIntegrity(ValidationResult result) {
        Long orphanCustomers = queryLong("SELECT COUNT(*) FROM customers c LEFT JOIN people p ON c.id = p.id WHERE p.id IS NULL");
        Long orphanEmployees = queryLong("SELECT COUNT(*) FROM employees e LEFT JOIN people p ON e.id = p.id WHERE p.id IS NULL");
        Long orphanAccounts = queryLong("SELECT COUNT(*) FROM user_accounts ua LEFT JOIN people p ON ua.id = p.id WHERE p.id IS NULL");

        if (orphanCustomers != null && orphanCustomers > 0) {
            result.addError("Orphan customers: " + orphanCustomers);
        }
        if (orphanEmployees != null && orphanEmployees > 0) {
            result.addError("Orphan employees: " + orphanEmployees);
        }
        if (orphanAccounts != null && orphanAccounts > 0) {
            result.addError("Orphan user accounts: " + orphanAccounts);
        }
    }

    private void validateDataSample(ValidationResult result) {
        Long badContracts = queryLong("SELECT COUNT(*) FROM rental_contracts rc LEFT JOIN customers c ON rc.customer_id = c.id WHERE c.id IS NULL");
        if (badContracts != null && badContracts > 0) {
            result.addError("Contracts without customer: " + badContracts);
        }
    }

    private Long queryLong(String sql) {
        try {
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            log.warn("Could not execute validation query: {}", sql, e);
            return null;
        }
    }

    private void logValidationResults(ValidationResult result) {
        log.info("MIGRATION VALIDATION RESULTS");
        log.info("Status: {}", result.isSuccessful() ? "SUCCESS" : "FAILED");
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
    }

    public static class ValidationResult {
        private boolean successful;
        private long customerCount;
        private long employeeCount;
        private long userAccountCount;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isSuccessful() { return successful; }
        public long getCustomerCount() { return customerCount; }
        public long getEmployeeCount() { return employeeCount; }
        public long getUserAccountCount() { return userAccountCount; }
        public int getErrorCount() { return errors.size(); }
        public int getWarningCount() { return warnings.size(); }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }

        public void setSuccessful(boolean successful) { this.successful = successful; }
        public void setCustomerCount(long count) { this.customerCount = count; }
        public void setEmployeeCount(long count) { this.employeeCount = count; }
        public void setUserAccountCount(long count) { this.userAccountCount = count; }
    }
}
