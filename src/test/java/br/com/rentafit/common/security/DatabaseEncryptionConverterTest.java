package br.com.rentafit.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseEncryptionConverterTest {

    @Mock
    private AesCryptoService aesCryptoService;

    private DatabaseEncryptionConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DatabaseEncryptionConverter();
        converter.setAesCryptoService(aesCryptoService);
    }

    @Test
    @DisplayName("Should encrypt value when converting to database column")
    void convertToDatabaseColumn() {
        when(aesCryptoService.encrypt("plain")).thenReturn("encrypted");

        String result = converter.convertToDatabaseColumn("plain");

        assertThat(result).isEqualTo("encrypted");
    }

    @Test
    @DisplayName("Should decrypt value when converting to entity attribute")
    void convertToEntityAttribute() {
        when(aesCryptoService.decrypt("encrypted")).thenReturn("plain");

        String result = converter.convertToEntityAttribute("encrypted");

        assertThat(result).isEqualTo("plain");
    }

    @Test
    @DisplayName("Should return null for null inputs")
    void convert_null() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}

