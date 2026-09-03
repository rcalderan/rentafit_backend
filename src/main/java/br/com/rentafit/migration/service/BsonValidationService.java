package br.com.rentafit.migration.service;

import br.com.rentafit.migration.config.MigrationProperties;
import br.com.rentafit.migration.dto.MigrationFileDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BsonValidationService {

    private static final Logger log = LoggerFactory.getLogger(BsonValidationService.class);

    private final MigrationProperties migrationProperties;

    public void validateSessionFiles(Path sessionPath, List<MigrationFileDTO> files) {
        for (MigrationFileDTO file : files) {
            if (!"bson".equals(file.getType())) {
                file.setStatus("ignored");
                continue;
            }

            Path filePath = sessionPath.resolve(file.getName());
            try {
                validateBsonFile(filePath);
                file.setStatus("valid");
                file.setError(null);
            } catch (Exception e) {
                file.setStatus("invalid");
                file.setError("BSON parse error: " + e.getMessage());
                log.warn("Invalid BSON file: {}", file.getName(), e);
            }
        }
    }

    private void validateBsonFile(Path filePath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                migrationProperties.getPythonExecutable(),
                "-c",
                "import sys, bson; bson.decode_all(open(sys.argv[1], 'rb').read())",
                filePath.toString()
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("BSON validation timed out");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("BSON validation failed with exit code " + process.exitValue());
        }
    }

    public boolean allBsonValid(List<MigrationFileDTO> files) {
        return files.stream()
                .filter(f -> "bson".equals(f.getType()))
                .allMatch(f -> "valid".equals(f.getStatus()));
    }
}
