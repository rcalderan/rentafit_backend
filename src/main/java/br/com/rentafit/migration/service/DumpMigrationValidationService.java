package br.com.rentafit.migration.service;

import br.com.rentafit.migration.validator.MigrationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DumpMigrationValidationService {

    private final DataSourceProperties dataSourceProperties;

    public MigrationValidator.ValidationResult validateDump() {
        JdbcTemplate dumpTemplate = dumpJdbcTemplate();
        return new MigrationValidator(dumpTemplate).validate();
    }

    private JdbcTemplate dumpJdbcTemplate() {
        String url = dataSourceProperties.determineUrl();
        String dumpUrl = url.replaceAll("(/[^/]+?)$", "/rentafit_dump");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(dumpUrl);
        dataSource.setUsername(dataSourceProperties.determineUsername());
        dataSource.setPassword(dataSourceProperties.determinePassword());
        return new JdbcTemplate(dataSource);
    }
}
