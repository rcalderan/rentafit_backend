package br.com.rentafit.migration.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para BsonFileReader
 */
class BsonFileReaderTest {

    private static final Logger log = LoggerFactory.getLogger(BsonFileReaderTest.class);
    private final BsonFileReader reader = new BsonFileReader();

    @Test
    void testReadBsonFile_FileNotFound() throws IOException {
        List<Map<String, Object>> result = reader.readBsonFile("nonexistent.bson");
        assertTrue(result.isEmpty(), "Should return empty list for non-existent file");
    }

    @Test
    void testReadBsonFile_EmptyFile(@TempDir Path tempDir) throws IOException {
        Path emptyFile = tempDir.resolve("empty.bson");
        Files.createFile(emptyFile);

        List<Map<String, Object>> result = reader.readBsonFile(emptyFile.toString());
        assertTrue(result.isEmpty(), "Should return empty list for empty file");
    }

    @Test
    void testReadRealBsonFile_Cliente() {
        // Teste com arquivo real se existir
        File clienteFile = new File(".legado/noivabd/cliente.bson");
        
        if (clienteFile.exists()) {
            try {
                log.info("Testing with real cliente.bson file");
                List<Map<String, Object>> documents = reader.readBsonFile(clienteFile.getAbsolutePath());
                
                log.info("Read {} documents from cliente.bson", documents.size());
                
                if (!documents.isEmpty()) {
                    Map<String, Object> firstDoc = documents.get(0);
                    log.info("First document keys: {}", firstDoc.keySet());
                    
                    // Verificar campos esperados
                    assertNotNull(firstDoc.get("_id"), "Document should have _id field");
                    
                    // Log sample data
                    if (firstDoc.containsKey("nome")) {
                        log.info("Sample nome: {}", firstDoc.get("nome"));
                    }
                }
            } catch (IOException e) {
                fail("Failed to read real BSON file: " + e.getMessage());
            }
        } else {
            log.warn("cliente.bson not found, skipping real file test");
        }
    }

    @Test
    void testReadRealBsonFile_Funcionario() {
        // Teste com arquivo real se existir
        File funcionarioFile = new File(".legado/noivabd/funcionario.bson");
        
        if (funcionarioFile.exists()) {
            try {
                log.info("Testing with real funcionario.bson file");
                List<Map<String, Object>> documents = reader.readBsonFile(funcionarioFile.getAbsolutePath());
                
                log.info("Read {} documents from funcionario.bson", documents.size());
                
                if (!documents.isEmpty()) {
                    Map<String, Object> firstDoc = documents.get(0);
                    log.info("First document keys: {}", firstDoc.keySet());
                    
                    // Verificar campos esperados
                    assertNotNull(firstDoc.get("_id"), "Document should have _id field");
                    
                    // Log sample data
                    if (firstDoc.containsKey("nome")) {
                        log.info("Sample nome: {}", firstDoc.get("nome"));
                    }
                    if (firstDoc.containsKey("usuario")) {
                        log.info("Sample usuario: {}", firstDoc.get("usuario"));
                    }
                }
            } catch (IOException e) {
                fail("Failed to read real BSON file: " + e.getMessage());
            }
        } else {
            log.warn("funcionario.bson not found, skipping real file test");
        }
    }
}
