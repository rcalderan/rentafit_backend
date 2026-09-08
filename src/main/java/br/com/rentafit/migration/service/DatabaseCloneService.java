package br.com.rentafit.migration.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

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
        terminateConnections("rentafit_dump");

        int originalLimit = queryAdmin("SELECT COALESCE(datconnlimit, -1) FROM pg_database WHERE datname = 'rentafit'", Integer.class);
        try {
            executeAdmin("ALTER DATABASE rentafit CONNECTION LIMIT 0");
            terminateConnections("rentafit");
            executeAdmin("DROP DATABASE IF EXISTS rentafit_dump");
            createDumpWithRetry();
        } finally {
            executeAdmin("ALTER DATABASE rentafit CONNECTION LIMIT " + originalLimit);
        }
    }

    private void createDumpWithRetry() {
        int attempts = 0;
        Exception lastError = null;
        while (attempts < 3) {
            attempts++;
            try {
                terminateConnections("rentafit");
                executeAdmin("CREATE DATABASE rentafit_dump WITH TEMPLATE rentafit OWNER postgres");
                return;
            } catch (Exception e) {
                lastError = e;
                log.warn("Attempt {} to create rentafit_dump failed: {}", attempts, e.getMessage());
                sleep(500);
            }
        }
        throw new IllegalStateException("Failed to create rentafit_dump after " + attempts + " attempts", lastError);
    }

    private void terminateConnections(String databaseName) {
        executeAdmin("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + databaseName + "' AND pid <> pg_backend_pid()");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean dumpExists() {
        Integer count = queryAdmin("SELECT COUNT(*) FROM pg_database WHERE datname = 'rentafit_dump'", Integer.class);
        return count != null && count > 0;
    }

    public String backupOriginal() {
        String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupName = "rentafit_backup_" + suffix;

        int originalLimit = queryAdmin("SELECT COALESCE(datconnlimit, -1) FROM pg_database WHERE datname = 'rentafit'", Integer.class);
        try {
            executeAdmin("ALTER DATABASE rentafit CONNECTION LIMIT 0");
            terminateConnections("rentafit");
            executeAdmin("CREATE DATABASE " + backupName + " WITH TEMPLATE rentafit OWNER postgres");
            log.info("Created backup database: {}", backupName);
            return backupName;
        } finally {
            executeAdmin("ALTER DATABASE rentafit CONNECTION LIMIT " + originalLimit);
        }
    }

    public void promoteDumpToOriginal() {
        terminateConnections("rentafit_old");
        terminateConnections("rentafit");
        terminateConnections("rentafit_dump");

        executeAdmin("DROP DATABASE IF EXISTS rentafit_old");

        terminateConnections("rentafit");
        terminateConnections("rentafit_dump");
        executeAdmin("ALTER DATABASE rentafit RENAME TO rentafit_old");
        executeAdmin("ALTER DATABASE rentafit_dump RENAME TO rentafit");

        executeAdmin("ALTER DATABASE rentafit CONNECTION LIMIT -1");
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
