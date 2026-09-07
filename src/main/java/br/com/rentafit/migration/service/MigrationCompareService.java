package br.com.rentafit.migration.service;

import br.com.rentafit.migration.dto.MigrationComparisonDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MigrationCompareService {

    private static final Logger log = LoggerFactory.getLogger(MigrationCompareService.class);

    private final DataSourceProperties dataSourceProperties;

    private static final List<String> TABLES = List.of(
            "people", "customers", "employees", "user_accounts", "categories",
            "products", "rental_items", "rental_contracts", "rental_contract_items",
            "rental_payments", "rental_contract_item_meta"
    );

    public MigrationComparisonDTO compare() {
        MigrationComparisonDTO result = new MigrationComparisonDTO();
        boolean allEqual = true;

        for (String table : TABLES) {
            long original = count("rentafit", table);
            long dump = count("rentafit_dump", table);
            long difference = dump - original;

            MigrationComparisonDTO.TableComparisonDTO tableComparison = new MigrationComparisonDTO.TableComparisonDTO();
            tableComparison.setTableName(table);
            tableComparison.setOriginalCount(original);
            tableComparison.setDumpCount(dump);
            tableComparison.setDifference(difference);
            result.getTables().add(tableComparison);

            if (difference != 0) {
                allEqual = false;
            }
        }

        result.setEqual(allEqual);
        return result;
    }

    private long count(String database, String table) {
        String sql = "SELECT COUNT(*) FROM " + database + ".\"" + table + "\"";
        try {
            JdbcTemplate template = adminTemplate();
            Long count = template.queryForObject(sql, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Could not count table {}.{}: {}", database, table, e.getMessage());
            return -1L;
        }
    }

    private JdbcTemplate adminTemplate() {
        String url = dataSourceProperties.determineUrl();
        String adminUrl = url.replaceAll("(/[^/]+?)$", "/postgres");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(adminUrl);
        dataSource.setUsername(dataSourceProperties.determineUsername());
        dataSource.setPassword(dataSourceProperties.determinePassword());
        return new JdbcTemplate(dataSource);
    }
}
