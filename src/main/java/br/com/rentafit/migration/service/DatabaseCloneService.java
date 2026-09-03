package br.com.rentafit.migration.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Serviço para clonar, fazer backup e promover o banco de dados PostgreSQL.
 * Operações DDL são executadas contra o banco administrativo 'postgres'.
 */
@Service
@RequiredArgsConstructor
public class DatabaseCloneService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCloneService.class);

    private final DataSourceProperties dataSourceProperties;

    public void cloneRentafitToDump() {
        executeAdmin("DROP DATABASE IF EXISTS rentafit_dump");
        executeAdmin("CREATE DATABASE rentafit_dump WITH TEMPLATE rentafit OWNER postgres");
    }

    public boolean dumpExists() {
        Integer count = queryAdmin("SELECT COUNT(*) FROM pg_database WHERE datname = 'rentafit_dump'", Integer.class);
        return count != null && count > 0;
    }

    public void backupOriginal() {
        String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupName = "rentafit_backup_" + suffix;
        executeAdmin("CREATE DATABASE " + backupName + " WITH TEMPLATE rentafit OWNER postgres");
        log.info("Created backup database: {}", backupName);
    }

    public void promoteDumpToOriginal() {
        executeAdmin("DROP DATABASE IF EXISTS rentafit_old");
        executeAdmin("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname IN ('rentafit', 'rentafit_dump') AND pid <> pg_backend_pid()");
        executeAdmin("ALTER DATABASE rentafit RENAME TO rentafit_old");
        executeAdmin("ALTER DATABASE rentafit_dump RENAME TO rentafit");
        log.info("Promoted rentafit_dump to rentafit");
    }

    private void executeAdmin(String sql) {
        JdbcTemplate template = adminTemplate();
        template.execute(sql);
    }

    private <T> T queryAdmin(String sql, Class<T> requiredType) {
        JdbcTemplate template = adminTemplate();
        return template.queryForObject(sql, requiredType);
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
