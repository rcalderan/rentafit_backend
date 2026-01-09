package br.com.rentafit.common.security;

import br.com.rentafit.common.util.BeanUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DatabaseEncryptionConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return getService().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return getService().decrypt(dbData);
    }

    private AesCryptoService getService() {
        return BeanUtil.getBean(AesCryptoService.class);
    }
}

