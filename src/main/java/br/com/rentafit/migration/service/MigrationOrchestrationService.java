package br.com.rentafit.migration.service;

import br.com.rentafit.migration.config.MigrationProperties;
import br.com.rentafit.migration.dto.MigrationFileDTO;
import br.com.rentafit.migration.dto.MigrationReportDTO;
import br.com.rentafit.migration.validator.MigrationValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MigrationOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(MigrationOrchestrationService.class);

    private final MigrationSessionService sessionService;
    private final BsonValidationService validationService;
    private final DatabaseCloneService cloneService;
    private final MigrationRunnerService runnerService;
    private final MigrationReportService reportService;
    private final DatabasePromotionService promotionService;
    private final DumpMigrationValidationService dumpValidationService;
    private final MigrationProperties migrationProperties;

    @Async("migrationTaskExecutor")
    public void runMigrationFlow(String sessionId) {
        try {
            transition(sessionId, "uploaded", "migrating", "Iniciando migracao");

            Path sessionPath = sessionService.resolveSessionPath(sessionId);
            List<MigrationFileDTO> files = sessionService.listFiles(sessionId);

            log.info("Validating BSON dump for session {}", sessionId);
            validationService.validateSessionFiles(sessionPath, files);
            if (!validationService.allBsonValid(files)) {
                String error = extractFirstError(files);
                fail(sessionId, "Validation failed: " + error);
                return;
            }

            log.info("Cloning rentafit to rentafit_dump for session {}", sessionId);
            cloneService.cloneRentafitToDump();

            log.info("Running Python migration for session {}", sessionId);
            MigrationReportDTO report = runnerService.runMigration(sessionId, sessionPath);

            transition(sessionId, "migrating", "validating", "Validando dados migrados");

            var validationResult = validateDump();
            List<String> validationErrors = validationResult.getErrors();
            List<String> validationWarnings = validationResult.getWarnings();

            report.getErrors().addAll(validationErrors);
            report.getWarnings().addAll(validationWarnings);
            sessionService.saveReport(sessionId, report);

            if (!validationResult.isSuccessful()) {
                String error = String.join("; ", validationErrors);
                fail(sessionId, "Validation failed: " + error);
                return;
            }

            transition(sessionId, "validating", "valid", "Migracao validada; aguardando promote");
            log.info("Migration completed successfully for session {}", sessionId);

        } catch (Exception e) {
            log.error("Migration flow failed for session {}", sessionId, e);
            fail(sessionId, e.getMessage());
        }
    }

    public void promote(String sessionId, boolean confirmed) {
        if (!confirmed) {
            throw new IllegalArgumentException("Promote requer confirmacao explicita (confirm=true).");
        }

        String status = sessionService.readStatus(sessionId);
        if (!"valid".equals(status)) {
            throw new IllegalStateException(
                    "Promote nao permitido. Status atual: " + status + ". Esperado: valid."
            );
        }

        try {
            transition(sessionId, status, "promoting", "Promovendo rentafit_dump para rentafit");
            promotionService.promote();
            transition(sessionId, "promoting", "promoted", "Banco promovido com sucesso");
        } catch (Exception e) {
            log.error("Promote failed for session {}", sessionId, e);
            fail(sessionId, "Promote failed: " + e.getMessage());
            throw new IllegalStateException("Promote failed for session " + sessionId, e);
        }
    }

    private MigrationValidator.ValidationResult validateDump() {
        return dumpValidationService.validateDump();
    }

    private void transition(String sessionId, String expected, String next, String message) throws IOException {
        String current = sessionService.readStatus(sessionId);
        if (!expected.equals(current)) {
            throw new IllegalStateException(
                    "Transicao invalida de " + current + " para " + next
            );
        }
        sessionService.saveStatus(sessionId, next, message);
        log.info("Session {}: {} -> {} - {}", sessionId, current, next, message);
    }

    private void fail(String sessionId, String message) {
        try {
            sessionService.saveStatus(sessionId, "failed", message);
        } catch (IOException e) {
            log.error("Could not save failure status for session {}", sessionId, e);
        }
    }

    private String extractFirstError(List<MigrationFileDTO> files) {
        return files.stream()
                .filter(f -> f.getError() != null && !f.getError().isBlank())
                .map(MigrationFileDTO::getError)
                .findFirst()
                .orElse("unknown validation error");
    }
}
