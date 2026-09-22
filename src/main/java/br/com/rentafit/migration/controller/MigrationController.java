package br.com.rentafit.migration.controller;

import br.com.rentafit.migration.dto.MigrationComparisonDTO;
import br.com.rentafit.migration.dto.MigrationReportDTO;
import br.com.rentafit.migration.dto.MigrationSessionDTO;
import br.com.rentafit.migration.service.MigrationCompareService;
import br.com.rentafit.migration.service.MigrationOrchestrationService;
import br.com.rentafit.migration.service.MigrationReportService;
import br.com.rentafit.migration.service.MigrationSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/migration")
@RequiredArgsConstructor
@Tag(name = "Migração", description = "Upload de BSON, execução de migração e promoção de banco")
public class MigrationController {

    private final MigrationSessionService sessionService;
    private final MigrationOrchestrationService orchestrationService;
    private final MigrationReportService reportService;
    private final MigrationCompareService compareService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload do dump BSON", description = "Envia o arquivo unico .bson e inicia migrate + validation em background")
    public ResponseEntity<MigrationSessionDTO> upload(@RequestParam("file") @NotNull MultipartFile file) throws IOException {
        MigrationSessionDTO session = sessionService.createSession();
        sessionService.storeFile(session.getId(), file);
        sessionService.saveStatus(session.getId(), "uploaded", null);

        orchestrationService.runMigrationFlow(session.getId());

        return ResponseEntity.ok(sessionService.getSession(session.getId()));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Consultar sessão", description = "Retorna status atual, arquivos e relatório da sessão")
    public ResponseEntity<MigrationSessionDTO> getSession(@PathVariable String sessionId) throws IOException {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/report")
    @Operation(summary = "Obter relatório", description = "Lê o report.json gerado pela migração")
    public ResponseEntity<MigrationReportDTO> getReport(@PathVariable String sessionId) throws IOException {
        Path outputPath = sessionService.resolveOutputPath(sessionId);
        Path reportPath = outputPath.resolve("report.json");
        if (!reportPath.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reportService.buildReport(reportPath));
    }

    @PostMapping("/sessions/{sessionId}/promote")
    @Operation(summary = "Promover dump", description = "Faz backup de rentafit e renomeia rentafit_dump para rentafit. Requer confirm=true")
    public ResponseEntity<Map<String, String>> promote(
            @PathVariable String sessionId,
            @RequestParam(name = "confirm", defaultValue = "false") boolean confirm) {
        orchestrationService.promote(sessionId, confirm);
        return ResponseEntity.ok(Map.of("status", "promoted", "message", "rentafit_dump promovido para rentafit"));
    }

    @GetMapping("/compare")
    @Operation(summary = "Comparar bancos", description = "Compara contagens entre rentafit, rentafit_dump e fontes MongoDB")
    public ResponseEntity<MigrationComparisonDTO> compare() {
        return ResponseEntity.ok(compareService.compare());
    }
}
