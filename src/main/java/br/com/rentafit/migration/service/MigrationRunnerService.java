package br.com.rentafit.migration.service;

import br.com.rentafit.migration.config.MigrationProperties;
import br.com.rentafit.migration.dto.MigrationReportDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MigrationRunnerService {

    private static final Logger log = LoggerFactory.getLogger(MigrationRunnerService.class);

    private final MigrationProperties migrationProperties;
    private final MigrationReportService reportService;
    private final DataSourceProperties dataSourceProperties;

    public MigrationReportDTO runMigration(String sessionId, Path sessionPath) throws Exception {
        Path outputPath = migrationProperties.resolveOutputPath().resolve(sessionId);
        Files.createDirectories(outputPath);

        Path script = migrationProperties.resolveScriptPath();
        if (!Files.exists(script)) {
            throw new IllegalStateException("Migration script not found: " + script);
        }

        ProcessBuilder pb = new ProcessBuilder(
                migrationProperties.getPythonExecutable(),
                script.toString(),
                "--session-dir", sessionPath.toString(),
                "--output-dir", outputPath.toString(),
                "--pg-host", resolvePgHost(),
                "--pg-port", resolvePgPort(),
                "--pg-db", "rentafit_dump",
                "--pg-user", dataSourceProperties.determineUsername(),
                "--pg-password", dataSourceProperties.determinePassword()
        );
        pb.inheritIO();
        pb.redirectErrorStream(true);

        log.info("Starting Python migration for session {} from script {}", sessionId, script);
        Process process = pb.start();
        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Python migration timed out after 10 minutes");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Python migration failed with exit code " + process.exitValue());
        }

        Path reportPath = outputPath.resolve("report.json");
        return reportService.buildReport(reportPath);
    }

    private String resolvePgHost() {
        String url = dataSourceProperties.determineUrl();
        // jdbc:postgresql://postgres:5432/rentafit -> postgres
        String host = url.replaceAll("jdbc:postgresql://([^:/]+).*", "$1");
        return host;
    }

    private String resolvePgPort() {
        String url = dataSourceProperties.determineUrl();
        // jdbc:postgresql://postgres:5432/rentafit -> 5432
        String port = url.replaceAll("jdbc:postgresql://[^:/]+:(\\d+).*", "$1");
        return port;
    }
}
