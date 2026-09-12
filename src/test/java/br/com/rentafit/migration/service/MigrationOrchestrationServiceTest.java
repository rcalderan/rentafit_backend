package br.com.rentafit.migration.service;

import br.com.rentafit.migration.config.MigrationProperties;
import br.com.rentafit.migration.dto.MigrationReportDTO;
import br.com.rentafit.migration.dto.MigrationSessionDTO;
import br.com.rentafit.migration.validator.MigrationValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MigrationOrchestrationServiceTest {

    private MigrationSessionService sessionService;
    private BsonValidationService validationService;
    private DatabaseCloneService cloneService;
    private MigrationRunnerService runnerService;
    private MigrationReportService reportService;
    private DatabasePromotionService promotionService;
    private DumpMigrationValidationService dumpValidationService;
    private MigrationProperties migrationProperties;

    private MigrationOrchestrationService orchestrationService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        migrationProperties = new MigrationProperties();
        migrationProperties.setBsonBasePath(tempDir.resolve("bson").toString());
        migrationProperties.setOutputPath(tempDir.resolve("output").toString());

        sessionService = new MigrationSessionService(migrationProperties, objectMapper());
        sessionService.init();

        validationService = mock(BsonValidationService.class);
        cloneService = mock(DatabaseCloneService.class);
        runnerService = mock(MigrationRunnerService.class);
        reportService = mock(MigrationReportService.class);
        promotionService = mock(DatabasePromotionService.class);
        dumpValidationService = mock(DumpMigrationValidationService.class);

        orchestrationService = new MigrationOrchestrationService(
                sessionService,
                validationService,
                cloneService,
                runnerService,
                reportService,
                promotionService,
                dumpValidationService,
                migrationProperties
        );
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("promote requer confirmacao explicita")
    void promoteRequiresExplicitConfirmation() throws IOException {
        MigrationSessionDTO session = sessionService.createSession();
        sessionService.saveStatus(session.getId(), "valid", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orchestrationService.promote(session.getId(), false)
        );

        assertTrue(ex.getMessage().contains("confirm=true"));
        verifyNoInteractions(promotionService);
    }

    @Test
    @DisplayName("promote exige status valid")
    void promoteRequiresValidStatus() throws IOException {
        MigrationSessionDTO session = sessionService.createSession();
        sessionService.saveStatus(session.getId(), "uploaded", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orchestrationService.promote(session.getId(), true)
        );

        assertTrue(ex.getMessage().contains("Status atual"));
        assertTrue(ex.getMessage().contains("valid"));
        verifyNoInteractions(promotionService);
    }

    @Test
    @DisplayName("promote com confirmacao e status valid executa promocao")
    void promoteWithConfirmationAndValidStatusExecutesPromotion() throws IOException {
        MigrationSessionDTO session = sessionService.createSession();
        sessionService.saveStatus(session.getId(), "valid", null);

        orchestrationService.promote(session.getId(), true);

        verify(promotionService).promote();
        assertEquals("promoted", sessionService.readStatus(session.getId()));
    }

    @Test
    @DisplayName("promote com falha mantem status failed")
    void promoteFailureKeepsFailedStatus() throws IOException {
        MigrationSessionDTO session = sessionService.createSession();
        sessionService.saveStatus(session.getId(), "valid", null);
        doThrow(new RuntimeException("DB error")).when(promotionService).promote();

        assertThrows(RuntimeException.class, () ->
                orchestrationService.promote(session.getId(), true)
        );

        assertEquals("failed", sessionService.readStatus(session.getId()));
    }

    @Test
    @DisplayName("fluxo de migracao falha quando validacao do dump falha")
    void migrationFlowFailsWhenBsonValidationFails() throws IOException {
        MigrationSessionDTO session = sessionService.createSession();
        sessionService.saveStatus(session.getId(), "uploaded", null);

        doAnswer(invocation -> {
            var files = invocation.<java.util.List<br.com.rentafit.migration.dto.MigrationFileDTO>>getArgument(1);
            for (var file : files) {
                file.setStatus("invalid");
                file.setError("formato invalido");
            }
            return null;
        }).when(validationService).validateSessionFiles(any(), any());

        when(validationService.allBsonValid(any())).thenReturn(false);

        orchestrationService.runMigrationFlow(session.getId());

        assertEquals("failed", sessionService.readStatus(session.getId()));
        verifyNoInteractions(cloneService);
    }

    @Test
    @DisplayName("fluxo de migracao completo com sucesso ate status valid")
    void migrationFlowCompletesSuccessfully() throws Exception {
        MigrationSessionDTO session = createSessionWithDummyBsonFile();
        sessionService.saveStatus(session.getId(), "uploaded", null);

        doAnswer(invocation -> {
            var files = invocation.<java.util.List<br.com.rentafit.migration.dto.MigrationFileDTO>>getArgument(1);
            for (var file : files) {
                file.setStatus("valid");
            }
            return null;
        }).when(validationService).validateSessionFiles(any(), any());
        when(validationService.allBsonValid(any())).thenReturn(true);

        MigrationReportDTO report = new MigrationReportDTO();
        report.setStatus("success");
        when(runnerService.runMigration(any(), any())).thenReturn(report);

        MigrationValidator.ValidationResult validationResult = new MigrationValidator.ValidationResult();
        validationResult.setSuccessful(true);
        when(dumpValidationService.validateDump()).thenReturn(validationResult);

        orchestrationService.runMigrationFlow(session.getId());

        assertEquals("valid", sessionService.readStatus(session.getId()));
        verify(cloneService).cloneRentafitToDump();
        verify(runnerService).runMigration(any(), any());
    }

    private MigrationSessionDTO createSessionWithDummyBsonFile() throws IOException {
        MigrationSessionDTO session = sessionService.createSession();
        java.nio.file.Files.write(
                sessionService.resolveSessionPath(session.getId()).resolve("noivabd_backup.bson"),
                new byte[]{0x00}
        );
        return session;
    }
}
