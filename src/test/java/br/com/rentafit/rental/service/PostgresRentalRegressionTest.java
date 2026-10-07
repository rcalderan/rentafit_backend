package br.com.rentafit.rental.service;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.auth.service.*;
import br.com.rentafit.config.InstallationMigrationConfiguration;
import br.com.rentafit.product.domain.enums.ProductStatus;
import br.com.rentafit.rental.adapter.RentalItemAdapter;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.port.*;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.validation.*;
import br.com.rentafit.settings.service.ApplicationSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RENTAFIT_REGRESSION_URL", matches = ".*/rentafit_regression")
@Import({InstallationMigrationConfiguration.class, InstallationCompletionService.class, UserAccountService.class,
        RefreshTokenService.class, RentalContractService.class, RentalRevisionService.class, RentalMapper.class,
        RentalWorkflowService.class, RentalReservationDelta.class, RentalContractValidator.class, ItemConflictChecker.class,
        RentalItemAdapter.class, ApplicationSettingsService.class, RentalProposalDuplication.class})
class PostgresRentalRegressionTest {
    @Autowired private JdbcTemplate database;
    @Autowired private InstallationCompletionService installation;
    @Autowired private RentalRevisionService revisions;
    @Autowired private jakarta.persistence.EntityManager entities;
    @Autowired private UserAccountRepository accounts;
    @Autowired private RentalContractService rentals;
    @Autowired private ApplicationSettingsService settings;
    @Autowired private RentalContractRepository contracts;
    @Autowired private PlatformTransactionManager transactions;
    @MockitoBean private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @MockitoBean private OperatorIdentityService operators;
    @MockitoBean private CurrentAccountId actor;
    @MockitoBean private CustomerPort customers;
    @MockitoBean private AccessoryPort accessories;
    private static final UUID BOOTSTRAP = UUID.fromString("0194269a-0000-7000-8000-000000000001");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("RENTAFIT_REGRESSION_URL"));
        properties.add("spring.datasource.driverClassName", () -> "org.postgresql.Driver");
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
    }

    @Test
    void completesFreshInstallationWithCascadesAndPreservesHistoricalPeople() {
        assertThat(database.queryForObject("SELECT current_database()", String.class)).isEqualTo("rentafit_regression");
        assertThat(database.queryForObject("SELECT status FROM installation_state", String.class)).isEqualTo("PENDING");
        UUID successor = createAdministrator();
        database.update("INSERT INTO refresh_tokens (id, token, expiry_date, user_account_id) VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '1 day', ?)",
                UUID.randomUUID(), "bootstrap-test-refresh", BOOTSTRAP);
        UserAccount authenticated = verifiedAdministrator(successor);
        installation.afterVerifiedLogin(authenticated);
        installation.afterVerifiedLogin(authenticated);
        assertThat(count("user_accounts", BOOTSTRAP)).isZero();
        assertThat(count("people", BOOTSTRAP)).isOne();
        assertThat(count("employees", BOOTSTRAP)).isOne();
        assertThat(count("user_accounts", successor)).isOne();
        assertThat(database.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE user_account_id = ?", Integer.class, BOOTSTRAP)).isZero();
        assertThat(database.queryForObject("SELECT COUNT(*) FROM user_roles WHERE user_id = ?", Integer.class, BOOTSTRAP)).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void retirementRollsBackIfOuterTransactionFails() {
        UUID successor = createAdministrator();
        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(status -> {
            installation.afterVerifiedLogin(verifiedAdministrator(successor));
            throw new IllegalStateException("forced rollback");
        })).hasMessage("forced rollback");
        assertThat(count("user_accounts", BOOTSTRAP)).isOne();
        assertThat(database.queryForObject("SELECT status FROM installation_state", String.class)).isEqualTo("PENDING");
    }

    @Test
    void updatingRevisionPreservesPaidIdsAndDoesNotViolateInstallmentUniqueness() {
        UUID originalId = createFinalizedContract();
        when(actor.requireId()).thenReturn(BOOTSTRAP);
        database.update("INSERT INTO rental_contract_items (id, contract_id, legacy_product_code, description, value, attendant_employee_id) VALUES (?, ?, 'fixture', 'Item', 15, ?)", UUID.randomUUID(), originalId, BOOTSTRAP);
        database.update("INSERT INTO rental_payments (id, contract_id, installment_number, payment_date, payment_method, value, installments, status) VALUES (?, ?, 2, CURRENT_DATE, 'PIX', 5, 1, 'PENDING')", UUID.randomUUID(), originalId);
        var revision = rentals.revise(originalId);
        UUID paidId = revision.payments().getFirst().id();
        var dto = new br.com.rentafit.rental.dto.UpdateRentalContractDTO(0, BOOTSTRAP, revision.pickupDate(),
                revision.eventDate(), revision.returnDate(), "updated", revision.items().stream().map(item ->
                new br.com.rentafit.rental.dto.ContractItemInputDTO(item.rentalItemId(), item.legacyProductCode(),
                        item.description(), item.value(), item.attendantEmployeeId(), java.util.List.of())).toList(),
                revision.payments().stream().map(payment -> new br.com.rentafit.rental.dto.RentalPaymentInputDTO(
                        payment.installmentNumber(), payment.paymentDate(), payment.paymentMethod(), payment.value(),
                        payment.installments(), payment.processedByEmployeeId(), payment.status())).toList());
        var updated = rentals.update(revision.id(), dto);
        contracts.flush();
        assertThat(updated.payments().getFirst().id()).isEqualTo(paidId);
        assertThat(updated.paidValue()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void finalizedRevisionRemainsFinalizedWithoutDuplicatingPayments() {
        UUID contractId = createFinalizedContract();
        when(actor.requireId()).thenReturn(BOOTSTRAP);
        var revision = rentals.revise(contractId);
        assertThat(revision.customerName()).isEqualTo("Historical customer");
        assertThat(revision.paidValue()).isEqualByComparingTo(BigDecimal.TEN);
        var confirmed = rentals.sign(revision.id());
        contracts.flush();
        assertThat(confirmed.status()).isEqualTo(ContractStatus.FINALIZED.getLegacyCode());
        assertThat(contracts.findById(contractId).orElseThrow().getStatus()).isEqualTo(ContractStatus.SUPERSEDED);
        assertThat(confirmed.paidValue()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void restartSynchronizesChangedOriginalBeforeConfirmingRevision() {
        UUID originalId = createFinalizedContract();
        when(actor.requireId()).thenReturn(BOOTSTRAP);
        var revision = rentals.revise(originalId);
        database.update("UPDATE rental_contracts SET notes = 'Updated original' WHERE id = ?", originalId);
        entities.clear();
        var restarted = revisions.restart(revision.id());
        assertThat(restarted.notes()).isEqualTo("Updated original");
        assertThat(restarted.paidValue()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(rentals.sign(revision.id()).status()).isEqualTo(ContractStatus.FINALIZED.getLegacyCode());
        contracts.flush();
    }

    @Test
    void configurationPersistsAndLegacyInstallationsDoNotRetireBootstrap() {
        when(actor.requireId()).thenReturn(BOOTSTRAP);
        settings.put(ApplicationSettingsService.RENTAL_WINDOW_KEY, "3");
        assertThat(settings.rentalWindowDays()).isEqualTo(3);
        database.update("UPDATE installation_state SET status = 'LEGACY' WHERE id = 1");
        installation.afterVerifiedLogin(verifiedAdministrator(createAdministrator()));
        assertThat(count("user_accounts", BOOTSTRAP)).isOne();
    }

    @Test
    void retiredBootstrapUsernameCannotBeReused() throws Exception {
        UUID successor = createAdministrator();
        installation.afterVerifiedLogin(verifiedAdministrator(successor));
        var connection = org.springframework.jdbc.datasource.DataSourceUtils.getConnection(database.getDataSource());
        var savepoint = connection.setSavepoint();
        assertThatThrownBy(() -> database.update("UPDATE user_accounts SET username = 'admin' WHERE id = ?", successor))
                .hasMessageContaining("Identidade de instalação retirada");
        connection.rollback(savepoint);
        assertThat(count("user_accounts", BOOTSTRAP)).isZero();
        assertThat(count("user_accounts", successor)).isOne();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSignaturesForSameItemAllowAtMostOneReservation() throws Exception {
        UUID itemId = createCatalogItem();
        UUID first = createItemDraft(itemId);
        UUID second = createItemDraft(itemId);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> attemptSign(first, ready, start));
            var two = executor.submit(() -> attemptSign(second, ready, start));
            assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(one.get(20, java.util.concurrent.TimeUnit.SECONDS),
                    two.get(20, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
    }

    private boolean attemptSign(UUID id, java.util.concurrent.CountDownLatch ready, java.util.concurrent.CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            rentals.sign(id);
            return true;
        } catch (br.com.rentafit.common.exception.ValidationException conflict) {
            return false;
        }
    }

    private UUID createItemDraft(UUID itemId) {
        UUID contractId = createFinalizedContract();
        database.update("UPDATE rental_contracts SET status = 'DRAFT' WHERE id = ?", contractId);
        database.update("INSERT INTO rental_contract_items (id, contract_id, rental_item_id, legacy_product_code, description, value, attendant_employee_id) VALUES (?, ?, ?, 'fixture', 'Concurrent item', 10, ?)",
                UUID.randomUUID(), contractId, itemId, BOOTSTRAP);
        return contractId;
    }

    private UUID createCatalogItem() {
        UUID id = UUID.randomUUID();
        UUID category = database.queryForObject("SELECT id FROM categories LIMIT 1", UUID.class);
        database.update("INSERT INTO products (id, category_id, name, value) VALUES (?, ?, 'Concurrent item', 10)", id, category);
        Integer code = database.queryForObject("SELECT COALESCE(MAX(legacy_id), 0) + 1 FROM rental_items", Integer.class);
        database.update("INSERT INTO rental_items (id, legacy_id, status) VALUES (?, ?, 'AVAILABLE')", id, code);
        return id;
    }

    private UserAccount verifiedAdministrator(UUID id) {
        UserAccount account = accounts.findById(id).orElseThrow();
        when(operators.profile(account.getUsername())).thenReturn(new br.com.rentafit.auth.dto.OperatorProfileResponseDTO(
                new br.com.rentafit.auth.dto.UserProfileResponseDTO(account, 90), "OP"));
        return account;
    }

    private UUID createAdministrator() {
        UUID id = UUID.randomUUID();
        database.update("INSERT INTO people (id, name, created_at, updated_at) VALUES (?, 'Definitive administrator', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", id);
        database.update("INSERT INTO employees (id, initials, role_level) VALUES (?, ?, 99)", id, id.toString().substring(0, 8));
        database.update("INSERT INTO user_accounts (id, username, password, pin, is_active, password_changed_at) VALUES (?, ?, 'test-hash', 'test-hash', true, CURRENT_TIMESTAMP)", id, id.toString());
        database.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE role = 'ADMIN'", id);
        return id;
    }

    private UUID createFinalizedContract() {
        UUID id = UUID.randomUUID();
        database.update("INSERT INTO customers (id, is_authenticated, notes) VALUES (?, false, '') ON CONFLICT DO NOTHING", BOOTSTRAP);
        database.update("INSERT INTO rental_contracts (id, legacy_id, customer_id, customer_name, customer_document, created_by_employee_id, pickup_date, event_date, return_date, status) VALUES (?, ?, ?, 'Historical customer', 'snapshot', ?, ?, ?, ?, 'FINALIZED')",
                id, "IT-" + id.toString().substring(0, 12), BOOTSTRAP, BOOTSTRAP, LocalDate.now().plusDays(2), LocalDate.now().plusDays(3), LocalDate.now().plusDays(4));
        database.update("INSERT INTO rental_payments (id, contract_id, installment_number, payment_date, payment_method, value, installments, processed_by_employee_id, status) VALUES (?, ?, 1, CURRENT_DATE, 'PIX', 10, 1, ?, 'PAID')", UUID.randomUUID(), id, BOOTSTRAP);
        return id;
    }

    private int count(String table, UUID id) {
        return database.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
    }
}
