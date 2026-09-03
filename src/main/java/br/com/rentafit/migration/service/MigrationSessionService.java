package br.com.rentafit.migration.service;

import br.com.rentafit.migration.config.MigrationProperties;
import br.com.rentafit.migration.dto.MigrationFileDTO;
import br.com.rentafit.migration.dto.MigrationSessionDTO;
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
        return session;
    }

    public Path resolveSessionPath(String sessionId) {
        return basePath.resolve(sessionId);
    }

    public Path resolveOutputPath(String sessionId) {
        return migrationProperties.resolveOutputPath().resolve(sessionId);
    }

    public MigrationSessionDTO getSession(String sessionId) throws IOException {
        Path sessionPath = resolveSessionPath(sessionId);
        if (!Files.exists(sessionPath)) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        MigrationSessionDTO session = new MigrationSessionDTO();
        session.setId(sessionId);
        session.setFiles(listFiles(sessionPath));
        session.setStatus(detectStatus(session));
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
        dto.setStatus("pending");
        return dto;
    }

    public List<MigrationFileDTO> listFiles(String sessionId) throws IOException {
        Path sessionPath = resolveSessionPath(sessionId);
        return listFiles(sessionPath);
    }

    private List<MigrationFileDTO> listFiles(Path sessionPath) throws IOException {
        if (!Files.exists(sessionPath)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(sessionPath)) {
            return stream
                    .filter(Files::isRegularFile)
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
        dto.setStatus("pending");
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

    private String detectStatus(MigrationSessionDTO session) {
        if (session.getFiles().isEmpty()) {
            return "created";
        }
        boolean hasBson = session.getFiles().stream().anyMatch(f -> "bson".equals(f.getType()));
        return hasBson ? "uploading" : "created";
    }

    private String sanitize(String originalName) {
        return originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
