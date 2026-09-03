package br.com.rentafit.migration.service;

import br.com.rentafit.migration.dto.MigrationReportDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MigrationReportService {

    private static final Logger log = LoggerFactory.getLogger(MigrationReportService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final DataSourceProperties dataSourceProperties;

    public MigrationReportDTO buildReport(Path reportPath) throws IOException {
        if (!reportPath.toFile().exists()) {
            throw new IOException("Report file not found: " + reportPath);
        }

        PythonReport pythonReport = objectMapper.readValue(reportPath.toFile(), PythonReport.class);

        MigrationReportDTO report = new MigrationReportDTO();
        report.setStartedAt(toOffsetDateTime(pythonReport.getStartedAt()));
        report.setFinishedAt(toOffsetDateTime(pythonReport.getFinishedAt()));
        report.setStatus(pythonReport.getStatus());
        report.setCustomersMigrated(countTable("customers"));
        report.setEmployeesMigrated(countTable("employees"));
        report.setCategoriesMigrated(countTable("categories"));
        report.setRentalItemsMigrated(countTable("rental_items"));
        report.setContractsMigrated(countTable("rental_contracts"));
        report.setErrors(new ArrayList<>(pythonReport.getErrors() != null ? pythonReport.getErrors() : List.of()));
        report.setWarnings(new ArrayList<>(pythonReport.getWarnings() != null ? pythonReport.getWarnings() : List.of()));
        return report;
    }

    private long countTable(String table) {
        try {
            JdbcTemplate dumpTemplate = dumpTemplate();
            Long value = dumpTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
            return value != null ? value : 0L;
        } catch (Exception e) {
            log.warn("Could not execute count query for {}: {}", table, e.getMessage());
            return 0L;
        }
    }

    private JdbcTemplate dumpTemplate() {
        String url = dataSourceProperties.determineUrl();
        String dumpUrl = url.replaceAll("(/[^/]+?)$", "/rentafit_dump");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(dumpUrl);
        dataSource.setUsername(dataSourceProperties.determineUsername());
        dataSource.setPassword(dataSourceProperties.determinePassword());
        return new JdbcTemplate(dataSource);
    }

    private OffsetDateTime toOffsetDateTime(Long epochSeconds) {
        if (epochSeconds == null) {
            return OffsetDateTime.now();
        }
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }

    public static class PythonReport {
        private String status;
        private Long startedAt;
        private Long finishedAt;
        private List<Map<String, Object>> tables;
        private List<String> errors;
        private List<String> warnings;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getStartedAt() { return startedAt; }
        public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }
        public Long getFinishedAt() { return finishedAt; }
        public void setFinishedAt(Long finishedAt) { this.finishedAt = finishedAt; }
        public List<Map<String, Object>> getTables() { return tables; }
        public void setTables(List<Map<String, Object>> tables) { this.tables = tables; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }
}
