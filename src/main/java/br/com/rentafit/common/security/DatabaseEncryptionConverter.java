package br.com.rentafit.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class DatabaseEncryptionConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(DatabaseEncryptionConverter.class);
    private static AesCryptoService aesCryptoService;

    @Autowired
    public void setAesCryptoService(AesCryptoService service) {
        DatabaseEncryptionConverter.aesCryptoService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        if (aesCryptoService == null) {
            log.warn("AesCryptoService not available, returning unencrypted value");
            return attribute;
        }
        try {
            return aesCryptoService.encrypt(attribute);
        } catch (Exception e) {
            log.error("Error encrypting data: {}", e.getMessage());
            return attribute; // Fallback: return original value
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if (aesCryptoService == null) {
            log.warn("AesCryptoService not available, returning encrypted value as-is");
            return dbData;
        }
        try {
            return aesCryptoService.decrypt(dbData);
        } catch (Exception e) {
            log.error("Error decrypting data: {}", e.getMessage());
            return dbData; // Fallback: return encrypted value
        }
    }
}

