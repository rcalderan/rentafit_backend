package br.com.rentafit.migration.service;

import br.com.rentafit.migration.config.MigrationProperties;
import br.com.rentafit.migration.dto.MigrationFileDTO;
import br.com.rentafit.migration.dto.MigrationSessionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitarios para MigrationSessionService usando diretorio temporario.
 * Simula upload de BSONs mock e valida status da sessao.
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

        service = new MigrationSessionService(properties);
        // @PostConstruct nao roda em teste unitario; chamar init manualmente
        service.init();
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
    @DisplayName("storeFile com BSON mock deve persistir arquivo e retornar status uploaded")
    void storeFilePersistsBsonAndReturnsUploadedStatus() throws IOException {
        MigrationSessionDTO session = service.createSession();
        byte[] bsonContent = createMinimalBson();

        MultipartFile mockFile = new MockMultipartFile(
                "file", "cliente.bson", "application/octet-stream", bsonContent
        );

        MigrationFileDTO dto = service.storeFile(session.getId(), mockFile);

        assertEquals("cliente.bson", dto.getName());
        assertEquals("bson", dto.getType());
        assertEquals("uploaded", dto.getStatus());
        assertEquals(bsonContent.length, dto.getSize());

        Path storedFile = service.resolveSessionPath(session.getId()).resolve("cliente.bson");
        assertTrue(Files.exists(storedFile));
    }

    @Test
    @DisplayName("getSession apos upload deve retornar status uploaded e arquivo com status uploaded")
    void getSessionAfterUploadReturnsUploadedStatus() throws IOException {
        MigrationSessionDTO session = service.createSession();
        byte[] bsonContent = createMinimalBson();

        MultipartFile mockFile = new MockMultipartFile(
                "file", "roupa.bson", "application/octet-stream", bsonContent
        );
        service.storeFile(session.getId(), mockFile);

        MigrationSessionDTO retrieved = service.getSession(session.getId());

        assertEquals("uploaded", retrieved.getStatus());
        assertEquals(1, retrieved.getFiles().size());
        MigrationFileDTO fileDto = retrieved.getFiles().get(0);
        assertEquals("roupa.bson", fileDto.getName());
        assertEquals("uploaded", fileDto.getStatus());
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
                "file", "cliente/test.bson", "application/octet-stream", bsonContent
        );

        MigrationFileDTO dto = service.storeFile(session.getId(), mockFile);

        // "/" deve ser substituido por "_"
        assertEquals("cliente_test.bson", dto.getName());
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
    @DisplayName("Multiplos uploads devem listar todos os arquivos na sessao")
    void multipleUploadsListAllFiles() throws IOException {
        MigrationSessionDTO session = service.createSession();
        byte[] bsonContent = createMinimalBson();

        for (String name : List.of("cliente.bson", "roupa.bson", "contrato.bson")) {
            MultipartFile mockFile = new MockMultipartFile(
                    "file", name, "application/octet-stream", bsonContent
            );
            service.storeFile(session.getId(), mockFile);
        }

        MigrationSessionDTO retrieved = service.getSession(session.getId());
        assertEquals(3, retrieved.getFiles().size());
        assertEquals("uploaded", retrieved.getStatus());
    }

    @Test
    @DisplayName("resolveOutputPath deve retornar caminho sob diretório de output")
    void resolveOutputPathReturnsPathUnderOutputDir() {
        Path outputPath = service.resolveOutputPath("session-123");

        assertNotNull(outputPath);
        assertTrue(outputPath.toString().contains("output"));
        assertTrue(outputPath.toString().contains("session-123"));
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
        // BSON: { _id: 1, nome: "test" }
        // _id: int32 = 0x10 0x00 0x00 0x00 _id 0x00 0x01 0x00 0x00 0x00
        // nome: string = 0x02 nome 0x00 0x05 0x00 0x00 0x00 test 0x00
        // total: 22 bytes + 4 bytes tamanho = 26
        return new byte[]{
                0x1A, 0x00, 0x00, 0x00,  // tamanho do documento = 26
                0x10,                     // tipo int32
                0x5F, 0x69, 0x64, 0x00,  // "_id\0"
                0x01, 0x00, 0x00, 0x00,  // valor 1
                0x02,                     // tipo string
                0x6E, 0x6F, 0x6D, 0x65, 0x00,  // "nome\0"
                0x05, 0x00, 0x00, 0x00,  // tamanho string = 5
                0x74, 0x65, 0x73, 0x74, 0x00,  // "test\0"
                0x00                      // fim do documento
        };
    }
}
