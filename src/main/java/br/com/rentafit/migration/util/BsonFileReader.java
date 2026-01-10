package br.com.rentafit.migration.util;

import org.bson.BsonBinaryReader;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.io.BasicOutputBuffer;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilitário para leitura de arquivos BSON gerados pelo mongodump
 * 
 * Lê arquivos .bson usando MongoDB BSON library e converte para Map<String, Object>
 * para facilitar o processamento nos ItemReaders
 */
@Component
public class BsonFileReader {

    private static final Logger log = LoggerFactory.getLogger(BsonFileReader.class);
    private static final BsonDocumentCodec CODEC = new BsonDocumentCodec();
    private static final JsonWriterSettings JSON_SETTINGS = JsonWriterSettings.builder()
        .outputMode(JsonMode.RELAXED)
        .build();

    /**
     * Lê todos os documentos de um arquivo BSON
     *
     * @param filePath Caminho para o arquivo .bson
     * @return Lista de mapas representando documentos BSON
     * @throws IOException Se houver erro ao ler o arquivo
     */
    public List<Map<String, Object>> readBsonFile(String filePath) throws IOException {
        List<Map<String, Object>> documents = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("BSON file not found: {}", filePath);
            return documents;
        }

        log.info("Reading BSON file: {} (Size: {} bytes)", filePath, file.length());

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            while (bis.available() > 0) {
                try {
                    // Ler o tamanho do documento (primeiros 4 bytes, little-endian)
                    byte[] sizeBytes = new byte[4];
                    int bytesRead = bis.read(sizeBytes);
                    
                    if (bytesRead < 4) {
                        break; // Fim do arquivo
                    }

                    int documentSize = ByteBuffer.wrap(sizeBytes)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .getInt();

                    if (documentSize <= 4 || documentSize > 16 * 1024 * 1024) {
                        log.warn("Invalid document size: {} bytes, skipping", documentSize);
                        break;
                    }

                    // Ler o documento completo
                    byte[] documentBytes = new byte[documentSize];
                    System.arraycopy(sizeBytes, 0, documentBytes, 0, 4);
                    
                    int remainingBytes = documentSize - 4;
                    int totalRead = 0;
                    
                    while (totalRead < remainingBytes) {
                        int read = bis.read(documentBytes, 4 + totalRead, remainingBytes - totalRead);
                        if (read == -1) {
                            break;
                        }
                        totalRead += read;
                    }

                    if (totalRead < remainingBytes) {
                        log.warn("Incomplete document read, expected {} bytes, got {} bytes", 
                            remainingBytes, totalRead);
                        break;
                    }

                    // Decodificar BSON para BsonDocument
                    ByteBuffer buffer = ByteBuffer.wrap(documentBytes);
                    BsonBinaryReader reader = new BsonBinaryReader(buffer);
                    BsonDocument bsonDoc = CODEC.decode(reader, DecoderContext.builder().build());
                    reader.close();

                    // Converter BsonDocument para Map<String, Object>
                    Map<String, Object> document = bsonDocumentToMap(bsonDoc);
                    documents.add(document);

                } catch (Exception e) {
                    log.error("Error parsing BSON document at position {}", documents.size(), e);
                    // Continuar lendo próximos documentos
                }
            }

            log.info("Successfully loaded {} documents from BSON file", documents.size());

        } catch (Exception e) {
            log.error("Error reading BSON file: {}", filePath, e);
            throw new IOException("Error reading BSON file: " + filePath, e);
        }

        return documents;
    }

    /**
     * Converte BsonDocument para Map<String, Object> recursivamente
     * 
     * @param bsonDoc Documento BSON
     * @return Map com valores convertidos para tipos Java
     */
    private Map<String, Object> bsonDocumentToMap(BsonDocument bsonDoc) {
        Map<String, Object> map = new HashMap<>();

        for (Map.Entry<String, BsonValue> entry : bsonDoc.entrySet()) {
            String key = entry.getKey();
            BsonValue value = entry.getValue();
            map.put(key, bsonValueToJava(value));
        }

        return map;
    }

    /**
     * Converte BsonValue para tipo Java correspondente
     * 
     * @param value Valor BSON
     * @return Objeto Java correspondente
     */
    private Object bsonValueToJava(BsonValue value) {
        if (value == null || value.isNull()) {
            return null;
        }

        return switch (value.getBsonType()) {
            case OBJECT_ID -> value.asObjectId().getValue().toHexString();
            case STRING -> value.asString().getValue();
            case INT32 -> value.asInt32().getValue();
            case INT64 -> value.asInt64().getValue();
            case DOUBLE -> value.asDouble().getValue();
            case BOOLEAN -> value.asBoolean().getValue();
            case DATE_TIME -> value.asDateTime().getValue(); // millis desde epoch
            case DOCUMENT -> bsonDocumentToMap(value.asDocument());
            case ARRAY -> {
                List<Object> list = new ArrayList<>();
                for (BsonValue item : value.asArray()) {
                    list.add(bsonValueToJava(item));
                }
                yield list;
            }
            case BINARY -> value.asBinary().getData();
            case UNDEFINED, NULL -> null;
            default -> value.toString(); // Fallback para outros tipos
        };
    }
}

