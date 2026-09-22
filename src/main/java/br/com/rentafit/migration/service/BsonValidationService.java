package br.com.rentafit.migration.service;

import br.com.rentafit.migration.config.MigrationProperties;
import br.com.rentafit.migration.dto.MigrationFileDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BsonValidationService {

    private static final Logger log = LoggerFactory.getLogger(BsonValidationService.class);

    private static final String VALIDATOR_SCRIPT = "migration/scripts/validate_dump.py";

    private final MigrationProperties migrationProperties;

    public void validateSessionFiles(Path sessionPath, List<MigrationFileDTO> files) {
        List<MigrationFileDTO> bsonFiles = files.stream()
                .filter(f -> "bson".equals(f.getType()))
                .toList();

        if (bsonFiles.isEmpty()) {
            throw new IllegalStateException("Nenhum arquivo .bson encontrado na sessao.");
        }
        if (bsonFiles.size() > 1) {
            for (MigrationFileDTO file : bsonFiles) {
                file.setStatus("invalid");
                file.setError("Formato antigo detectado: apenas o dump unico e aceito.");
            }
            return;
        }

        MigrationFileDTO file = bsonFiles.get(0);
        Path filePath = sessionPath.resolve(file.getName());
        try {
            validateSingleDump(filePath);
            file.setStatus("valid");
            file.setError(null);
        } catch (Exception e) {
            file.setStatus("invalid");
            file.setError("BSON validation error: " + e.getMessage());
            log.warn("Invalid BSON dump file: {}", file.getName(), e);
        }
    }

    public void validateSingleDump(Path filePath) throws IOException, InterruptedException {
        Path scriptPath = migrationProperties.resolveScriptPath().getParent().resolve("validate_dump.py");
        if (!scriptPath.toFile().exists()) {
            scriptPath = Path.of(VALIDATOR_SCRIPT).toAbsolutePath().normalize();
        }

        ProcessBuilder pb = new ProcessBuilder(
                migrationProperties.getPythonExecutable(),
                scriptPath.toString(),
                filePath.toString(),
                "--expected-collections", "cliente", "contrato", "roupa", "funcionario",
                "roupa_tipo", "conf", "fornecedor"
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(2, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("BSON validation timed out");
        }

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }

        if (process.exitValue() != 0) {
            throw new IllegalStateException("BSON validation failed: " + output);
        }
    }

    public boolean allBsonValid(List<MigrationFileDTO> files) {
        return files.stream()
                .filter(f -> "bson".equals(f.getType()))
                .allMatch(f -> "valid".equals(f.getStatus()));
    }
}
