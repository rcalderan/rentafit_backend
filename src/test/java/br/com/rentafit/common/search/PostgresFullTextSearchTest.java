package br.com.rentafit.common.search;

import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.repository.CustomerRepository;
import br.com.rentafit.product.domain.Category;
import br.com.rentafit.product.domain.RentalItem;
import br.com.rentafit.product.domain.RetailProduct;
import br.com.rentafit.product.repository.CategoryRepository;
import br.com.rentafit.product.repository.RentalItemRepository;
import br.com.rentafit.product.repository.RetailProductRepository;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.repository.RentalContractRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida o FTS Postgres (V39) de ponta a ponta: funções customer_search_vec,
 * product_search_vec, legacy_id_search_vec, retail_search_vec, contract_search_vec
 * + config pt_unaccent + índices GIN, através dos métodos searchByFullText.
 *
 * <p>Sem cobertura no H2 (to_tsvector não existe lá): roda só quando
 * RENTAFIT_REGRESSION_URL aponta para o Postgres de regressão.</p>
 */
@DataJpaTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.show-sql=false", "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RENTAFIT_REGRESSION_URL", matches = ".*/rentafit_regression")
class PostgresFullTextSearchTest {

    @Autowired private JdbcTemplate database;
    @Autowired private CustomerRepository customers;
    @Autowired private CategoryRepository categories;
    @Autowired private RentalItemRepository rentalItems;
    @Autowired private RetailProductRepository retailProducts;
    @Autowired private RentalContractRepository contracts;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("RENTAFIT_REGRESSION_URL"));
        properties.add("spring.datasource.driverClassName", () -> "org.postgresql.Driver");
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
    }

    @Test
    void customerSearchMatchesAccentInsensitiveAndRanksNameAboveEmail() {
        UUID byName = insertCustomer("José Silva", null);
        // email vira um único lexeme — casa só por prefixo do início do endereço
        UUID byEmail = insertCustomer("Maria Antonia", "jose.qualquer@example.com");

        Page<Customer> page = customers.searchByFullText("jose:*", "jose", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Customer::getId)
                .as("nome acentuado casa busca sem acento e pesa A > email peso C")
                .containsSubsequence(byName, byEmail);
    }

    @Test
    void customerSearchFallsBackToEmailMatch() {
        UUID target = insertCustomer("Fernanda Costa", "atelie.fernanda@example.com");
        insertCustomer("Outra Pessoa", "outra.pessoa@example.com");

        Page<Customer> page = customers.searchByFullText("atelie:*", "atelie", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Customer::getId).contains(target);
    }

    @Test
    void rentalItemSearchMatchesNameCategoryAndLegacyId() {
        UUID categoryId = insertCategory("FTS_CAT_VEST", "Vestidos de Festa");
        UUID itemId = insertRentalItem("Longo Vermelho", "Vera Wang", 777, categoryId);

        List<UUID> matchedCategories = categories.findIdsMatchingTsQuery("vest:*");
        assertThat(matchedCategories).contains(categoryId);

        assertThat(rentalItems.searchByFullText("vera:*", "vera",
                TsQueryBuilder.idsOrNeverMatch(matchedCategories), PageRequest.of(0, 10)).getContent())
                .as("busca por grife/marca").extracting(RentalItem::getId).contains(itemId);
        assertThat(rentalItems.searchByFullText("777", "777",
                List.of(new UUID(0, 0)), PageRequest.of(0, 10)).getContent())
                .as("busca por legacyId numérico").extracting(RentalItem::getId).contains(itemId);
        assertThat(rentalItems.searchByFullText("fest:*", "fest",
                TsQueryBuilder.idsOrNeverMatch(matchedCategories), PageRequest.of(0, 10)).getContent())
                .as("match apenas via categoria").extracting(RentalItem::getId).contains(itemId);
    }

    @Test
    void retailProductSearchMatchesSkuPrefix() {
        UUID categoryId = insertCategory("FTS_CAT_RETAIL", "Camisas Retail");
        UUID productId = insertRetailProduct("Camisa Social Azul", "SKU-98765", categoryId);

        Page<RetailProduct> page = retailProducts.searchByFullText("98765", "98765",
                List.of(new UUID(0, 0)), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(RetailProduct::getId).contains(productId);
    }

    @Test
    void contractSearchMatchesCustomerNameAndLegacyId() {
        UUID customerId = insertCustomer("Ana Beatriz Rocha", null);
        UUID contractId = insertContract("FTS-001", "Ana Beatriz Rocha", customerId);

        assertThat(contracts.searchByFullText("an:* & roch:*", "an | roch", PageRequest.of(0, 10)).getContent())
                .as("PREFIX_ALL por nome do cliente").extracting(RentalContract::getId).contains(contractId);
        assertThat(contracts.searchByFullText("001:*", "001", PageRequest.of(0, 10)).getContent())
                .as("segmento numérico de legacy_id hifenado").extracting(RentalContract::getId).contains(contractId);
    }


    @Test
    void exactWordMatchOutranksSharedStem() {
        UUID exact = insertCustomer("Claudio Souza", null);
        UUID stemOnly = insertCustomer("Claudiane Alves", null);

        // 'claudio' e 'claudiane' têm o mesmo stem 'claudi' — o boost raw_unaccent
        // é o que desempata a favor do match exato por palavra inteira
        Page<Customer> page = customers.searchByFullText("claudio:*", "claudio", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Customer::getId)
                .as("match exato deve rankear acima de stem compartilhado")
                .containsSubsequence(exact, stemOnly);
    }

    private UUID insertCustomer(String name, String email) {
        UUID id = UUID.randomUUID();
        database.update("INSERT INTO people (id, name, email) VALUES (?, ?, ?)", id, name, email);
        database.update("INSERT INTO customers (id) VALUES (?)", id);
        return id;
    }

    private UUID insertCategory(String name, String displayName) {
        UUID id = UUID.randomUUID();
        database.update("INSERT INTO categories (id, name, display_name, product_type, active, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'RENTAL', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", id, name, displayName);
        return id;
    }

    private UUID insertRentalItem(String name, String brand, int legacyId, UUID categoryId) {
        UUID id = UUID.randomUUID();
        database.update("INSERT INTO products (id, category_id, name, brand, value) VALUES (?, ?, ?, ?, 100)",
                id, categoryId, name, brand);
        database.update("INSERT INTO rental_items (id, legacy_id, status) VALUES (?, ?, 'AVAILABLE')", id, legacyId);
        return id;
    }

    private UUID insertRetailProduct(String name, String sku, UUID categoryId) {
        UUID id = UUID.randomUUID();
        database.update("INSERT INTO products (id, category_id, name, value) VALUES (?, ?, ?, 50)",
                id, categoryId, name);
        database.update("INSERT INTO retail_products (id, sku) VALUES (?, ?)", id, sku);
        return id;
    }

    private UUID insertContract(String legacyId, String customerName, UUID customerId) {
        UUID id = UUID.randomUUID();
        database.update("INSERT INTO rental_contracts (id, legacy_id, customer_name, customer_id, "
                        + "pickup_date, event_date, return_date) VALUES (?, ?, ?, ?, CURRENT_DATE, CURRENT_DATE, CURRENT_DATE)",
                id, legacyId, customerName, customerId);
        return id;
    }
}
