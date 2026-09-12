package br.com.rentafit.migration.service;

import br.com.rentafit.migration.config.MigrationProperties;
import br.com.rentafit.migration.dto.MigrationFileDTO;
import br.com.rentafit.migration.dto.MigrationReportDTO;
import br.com.rentafit.migration.dto.MigrationSessionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MigrationSessionService {

    private static final Logger log = LoggerFactory.getLogger(MigrationSessionService.class);

    private final MigrationProperties migrationProperties;
    private final ObjectMapper objectMapper;

    private Path basePath;

    @PostConstruct
    void init() throws IOException {
        basePath = migrationProperties.resolveBsonBasePath().resolve("sessions").toAbsolutePath();
        Files.createDirectories(basePath);
    }

    public MigrationSessionDTO createSession() throws IOException {
        String id = UUID.randomUUID().toString();
        Path sessionPath = resolveSessionPath(id);
        Files.createDirectories(sessionPath);

        MigrationSessionDTO session = new MigrationSessionDTO();
        session.setId(id);
        session.setCreatedAt(OffsetDateTime.now());
        session.setStatus("created");
        saveStatus(id, "created", null);
        return session;
    }

    public Path resolveSessionPath(String sessionId) {
        return basePath.resolve(sessionId);
    }

    public Path resolveOutputPath(String sessionId) {
        return migrationProperties.resolveOutputPath().resolve(sessionId);
    }

    private Path statusPath(String sessionId) {
        return resolveSessionPath(sessionId).resolve("status.json");
    }

    private Path reportPath(String sessionId) {
        return resolveOutputPath(sessionId).resolve("report.json");
    }

    public MigrationSessionDTO getSession(String sessionId) throws IOException {
        Path sessionPath = resolveSessionPath(sessionId);
        if (!Files.exists(sessionPath)) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        MigrationSessionDTO session = new MigrationSessionDTO();
        session.setId(sessionId);
        session.setFiles(listFiles(sessionPath));
        session.setStatus(readStatus(sessionId));
        if (reportPath(sessionId).toFile().exists()) {
            try {
                session.setReport(objectMapper.readValue(reportPath(sessionId).toFile(), MigrationReportDTO.class));
            } catch (IOException e) {
                log.warn("Could not read report for session {}", sessionId, e);
            }
        }
        return session;
    }

    public MigrationFileDTO storeFile(String sessionId, MultipartFile file) throws IOException {
        Path sessionPath = resolveSessionPath(sessionId);
        if (!Files.exists(sessionPath)) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        String safeName = sanitize(originalName);
        Path target = sessionPath.resolve(safeName);
        file.transferTo(target);

        MigrationFileDTO dto = new MigrationFileDTO();
        dto.setName(safeName);
        dto.setSize(file.getSize());
        dto.setType(detectType(safeName));
        dto.setStatus("uploaded");
        return dto;
    }

    public List<MigrationFileDTO> listFiles(String sessionId) throws IOException {
        Path sessionPath = resolveSessionPath(sessionId);
        return listFiles(sessionPath);
    }

    public void saveStatus(String sessionId, String status, String errorMessage) throws IOException {
        Path path = statusPath(sessionId);
        Files.createDirectories(path.getParent());
        objectMapper.writeValue(path.toFile(), new StatusSnapshot(sessionId, status, errorMessage));
    }

    public String readStatus(String sessionId) {
        Path path = statusPath(sessionId);
        if (!path.toFile().exists()) {
            return "created";
        }
        try {
            StatusSnapshot snapshot = objectMapper.readValue(path.toFile(), StatusSnapshot.class);
            return snapshot.getStatus();
        } catch (IOException e) {
            log.warn("Could not read status for session {}", sessionId, e);
            return "created";
        }
    }

    public String readError(String sessionId) {
        Path path = statusPath(sessionId);
        if (!path.toFile().exists()) {
            return null;
        }
        try {
            StatusSnapshot snapshot = objectMapper.readValue(path.toFile(), StatusSnapshot.class);
            return snapshot.getErrorMessage();
        } catch (IOException e) {
            return null;
        }
    }

    public void saveReport(String sessionId, MigrationReportDTO report) throws IOException {
        Path outputPath = resolveOutputPath(sessionId);
        Files.createDirectories(outputPath);
        objectMapper.writeValue(reportPath(sessionId).toFile(), report);
    }

    private List<MigrationFileDTO> listFiles(Path sessionPath) throws IOException {
        if (!Files.exists(sessionPath)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(sessionPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().equals("status.json"))
                    .map(this::toFileDto)
                    .toList();
        }
    }

    private MigrationFileDTO toFileDto(Path path) {
        MigrationFileDTO dto = new MigrationFileDTO();
        dto.setName(path.getFileName().toString());
        try {
            dto.setSize(Files.size(path));
        } catch (IOException e) {
            dto.setSize(0);
        }
        dto.setType(detectType(path.getFileName().toString()));
        dto.setStatus("uploaded");
        return dto;
    }

    private String detectType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".bson")) {
            return "bson";
        }
        if (lower.endsWith(".json")) {
            return "metadata";
        }
        return "unknown";
    }

    private String sanitize(String originalName) {
        return originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static class StatusSnapshot {
        private String sessionId;
        private String status;
        private String errorMessage;

        public StatusSnapshot() {
        }

        public StatusSnapshot(String sessionId, String status, String errorMessage) {
            this.sessionId = sessionId;
            this.status = status;
            this.errorMessage = errorMessage;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
