package br.com.rentafit.migration.controller;

import br.com.rentafit.migration.dto.MigrationComparisonDTO;
import br.com.rentafit.migration.dto.MigrationReportDTO;
import br.com.rentafit.migration.dto.MigrationSessionDTO;
import br.com.rentafit.migration.service.BsonValidationService;
import br.com.rentafit.migration.service.DatabaseCloneService;
import br.com.rentafit.migration.service.DatabasePromotionService;
import br.com.rentafit.migration.service.MigrationCompareService;
import br.com.rentafit.migration.service.MigrationReportService;
import br.com.rentafit.migration.service.MigrationRunnerService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/migration")
@RequiredArgsConstructor
@Tag(name = "Migração", description = "Upload de BSON, execução de migração e promoção de banco")
public class MigrationController {

    private final MigrationSessionService sessionService;
    private final BsonValidationService validationService;
    private final MigrationRunnerService runnerService;
    private final MigrationCompareService compareService;
    private final MigrationReportService reportService;
    private final DatabaseCloneService databaseCloneService;
    private final DatabasePromotionService promotionService;

    @PostMapping("/sessions")
    @Operation(summary = "Criar sessão de migração", description = "Gera um diretório temporário para anexar arquivos .bson")
    public ResponseEntity<MigrationSessionDTO> createSession() throws IOException {
        return ResponseEntity.ok(sessionService.createSession());
    }

    @PostMapping(value = "/sessions/{sessionId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Anexar arquivo", description = "Faz upload de um arquivo .bson ou .metadata.json na sessão")
    public ResponseEntity<MigrationSessionDTO> uploadFile(
            @PathVariable String sessionId,
            @RequestParam("file") @NotNull MultipartFile file) throws IOException {
        sessionService.storeFile(sessionId, file);
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Consultar sessão", description = "Lista arquivos anexados e status da sessão")
    public ResponseEntity<MigrationSessionDTO> getSession(@PathVariable String sessionId) throws IOException {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/validate")
    @Operation(summary = "Validar arquivos", description = "Valida se os arquivos .bson são parseáveis")
    public ResponseEntity<MigrationSessionDTO> validateSession(@PathVariable String sessionId) throws IOException {
        MigrationSessionDTO session = sessionService.getSession(sessionId);
        Path sessionPath = sessionService.resolveSessionPath(sessionId);
        validationService.validateSessionFiles(sessionPath, session.getFiles());
        session.setStatus(validationService.allBsonValid(session.getFiles()) ? "valid" : "invalid");
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/clone")
    @Operation(summary = "Clonar rentafit para rentafit_dump", description = "Cria o banco de testes a partir do original")
    public ResponseEntity<String> cloneDatabase() {
        databaseCloneService.cloneRentafitToDump();
        return ResponseEntity.ok("rentafit_dump criado a partir de rentafit");
    }

    @PostMapping("/sessions/{sessionId}/run")
    @Operation(summary = "Executar migração", description = "Roda o script Python a partir dos arquivos .bson da sessão")
    public ResponseEntity<MigrationReportDTO> runMigration(@PathVariable String sessionId) throws Exception {
        Path sessionPath = sessionService.resolveSessionPath(sessionId);
        MigrationReportDTO report = runnerService.runMigration(sessionId, sessionPath);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/compare")
    @Operation(summary = "Comparar bancos", description = "Compara contagens entre rentafit, rentafit_dump e fontes MongoDB")
    public ResponseEntity<MigrationComparisonDTO> compare() {
        return ResponseEntity.ok(compareService.compare());
    }

    @PostMapping("/backup")
    @Operation(summary = "Backup do original", description = "Cria um backup nomeado de rentafit")
    public ResponseEntity<String> backup() {
        databaseCloneService.backupOriginal();
        return ResponseEntity.ok("Backup criado");
    }

    @PostMapping("/promote")
    @Operation(summary = "Promover dump", description = "Faz backup de rentafit e renomeia rentafit_dump para rentafit")
    public ResponseEntity<String> promote() {
        promotionService.promote();
        return ResponseEntity.ok("rentafit_dump promovido para rentafit");
    }
}
