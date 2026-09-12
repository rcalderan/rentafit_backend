package br.com.rentafit.migration.service;

import br.com.rentafit.migration.config.MigrationProperties;
import br.com.rentafit.migration.dto.MigrationFileDTO;
import br.com.rentafit.migration.dto.MigrationReportDTO;
import br.com.rentafit.migration.dto.MigrationSessionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitarios para MigrationSessionService usando diretorio temporario.
 * Simula upload de BSON e valida status da sessao.
 */
class MigrationSessionServiceTest {

    private MigrationSessionService service;
    private MigrationProperties properties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        properties = new MigrationProperties();
        properties.setBsonBasePath(tempDir.resolve("bson").toString());
        properties.setOutputPath(tempDir.resolve("output").toString());

        service = new MigrationSessionService(properties, objectMapper());
        service.init();
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("createSession deve criar diretorio e retornar sessao com status created")
    void createSessionCreatesDirectoryAndReturnsCreatedStatus() throws IOException {
        MigrationSessionDTO session = service.createSession();

        assertNotNull(session.getId());
        assertEquals("created", session.getStatus());
        Path sessionPath = service.resolveSessionPath(session.getId());
        assertTrue(Files.exists(sessionPath));
    }

    @Test
    @DisplayName("storeFile com BSON deve persistir arquivo e retornar status uploaded")
    void storeFilePersistsBsonAndReturnsUploadedStatus() throws IOException {
        MigrationSessionDTO session = service.createSession();
        byte[] bsonContent = createMinimalBson();

        MultipartFile mockFile = new MockMultipartFile(
                "file", "noivabd_backup.bson", "application/octet-stream", bsonContent
        );

        MigrationFileDTO dto = service.storeFile(session.getId(), mockFile);

        assertEquals("noivabd_backup.bson", dto.getName());
        assertEquals("bson", dto.getType());
        assertEquals("uploaded", dto.getStatus());
        assertEquals(bsonContent.length, dto.getSize());

        Path storedFile = service.resolveSessionPath(session.getId()).resolve("noivabd_backup.bson");
        assertTrue(Files.exists(storedFile));
    }

    @Test
    @DisplayName("getSession apos upload deve retornar status salvo")
    void getSessionAfterUploadReturnsUploadedStatus() throws IOException {
        MigrationSessionDTO session = service.createSession();
        byte[] bsonContent = createMinimalBson();

        MultipartFile mockFile = new MockMultipartFile(
                "file", "noivabd_backup.bson", "application/octet-stream", bsonContent
        );
        service.storeFile(session.getId(), mockFile);
        service.saveStatus(session.getId(), "uploaded", null);

        MigrationSessionDTO retrieved = service.getSession(session.getId());

        assertEquals("uploaded", retrieved.getStatus());
        assertEquals(1, retrieved.getFiles().size());
        MigrationFileDTO fileDto = retrieved.getFiles().get(0);
        assertEquals("noivabd_backup.bson", fileDto.getName());
    }

    @Test
    @DisplayName("saveStatus e readStatus persistem e recuperam estado")
    void saveStatusAndReadStatusPersistState() throws IOException {
        MigrationSessionDTO session = service.createSession();

        service.saveStatus(session.getId(), "migrating", null);

        assertEquals("migrating", service.readStatus(session.getId()));
    }

    @Test
    @DisplayName("saveReport e getSession retornam relatorio salvo")
    void saveReportAndGetSessionReturnReport() throws IOException {
        MigrationSessionDTO session = service.createSession();
        MigrationReportDTO report = new MigrationReportDTO();
        report.setStatus("success");
        report.setStartedAt(OffsetDateTime.now());
        report.setFinishedAt(OffsetDateTime.now());
        report.setCustomersMigrated(10);

        service.saveReport(session.getId(), report);

        MigrationSessionDTO retrieved = service.getSession(session.getId());
        assertNotNull(retrieved.getReport());
        assertEquals("success", retrieved.getReport().getStatus());
        assertEquals(10, retrieved.getReport().getCustomersMigrated());
    }

    @Test
    @DisplayName("storeFile com nome invalido deve lancar IllegalArgumentException")
    void storeFileWithNullNameThrowsException() throws IOException {
        MigrationSessionDTO session = service.createSession();

        MultipartFile mockFile = new MockMultipartFile(
                "file", "", "application/octet-stream", new byte[]{1, 2, 3}
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.storeFile(session.getId(), mockFile)
        );
    }

    @Test
    @DisplayName("storeFile com sessionId inexistente deve lancar IllegalArgumentException")
    void storeFileWithInvalidSessionIdThrowsException() {
        MultipartFile mockFile = new MockMultipartFile(
                "file", "test.bson", "application/octet-stream", new byte[]{1, 2, 3}
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.storeFile("nonexistent-session", mockFile)
        );
    }

    @Test
    @DisplayName("storeFile deve sanitizar nome com caracteres especiais")
    void storeFileSanitizesSpecialCharacters() throws IOException {
        MigrationSessionDTO session = service.createSession();
        byte[] bsonContent = createMinimalBson();

        MultipartFile mockFile = new MockMultipartFile(
                "file", "backup/test.bson", "application/octet-stream", bsonContent
        );

        MigrationFileDTO dto = service.storeFile(session.getId(), mockFile);

        assertEquals("backup_test.bson", dto.getName());
    }

    @Test
    @DisplayName("getSession sem arquivos deve retornar status created")
    void getSessionWithNoFilesReturnsCreatedStatus() throws IOException {
        MigrationSessionDTO session = service.createSession();

        MigrationSessionDTO retrieved = service.getSession(session.getId());

        assertEquals("created", retrieved.getStatus());
        assertTrue(retrieved.getFiles().isEmpty());
    }

    @Test
    @DisplayName("listFiles por sessionId deve retornar arquivos da sessão")
    void listFilesBySessionIdReturnsFiles() throws IOException {
        MigrationSessionDTO session = service.createSession();
        byte[] bsonContent = createMinimalBson();

        MultipartFile mockFile = new MockMultipartFile(
                "file", "dados.bson", "application/octet-stream", bsonContent
        );
        service.storeFile(session.getId(), mockFile);

        List<MigrationFileDTO> files = service.listFiles(session.getId());

        assertEquals(1, files.size());
        assertEquals("dados.bson", files.get(0).getName());
        assertEquals("bson", files.get(0).getType());
    }

    @Test
    @DisplayName("listFiles por sessionId inexistente deve retornar lista vazia")
    void listFilesByInvalidSessionIdReturnsEmptyList() throws IOException {
        List<MigrationFileDTO> files = service.listFiles("nonexistent-session");

        assertTrue(files.isEmpty());
    }

    /**
     * Cria um BSON minimo valido: 1 documento com _id=1 e nome="test".
     * Formato: 4 bytes tamanho + documento BSON.
     */
    private byte[] createMinimalBson() {
        return new byte[]{
                0x1A, 0x00, 0x00, 0x00,
                0x10,
                0x5F, 0x69, 0x64, 0x00,
                0x01, 0x00, 0x00, 0x00,
                0x02,
                0x6E, 0x6F, 0x6D, 0x65, 0x00,
                0x05, 0x00, 0x00, 0x00,
                0x74, 0x65, 0x73, 0x74, 0x00,
                0x00
        };
    }
}
