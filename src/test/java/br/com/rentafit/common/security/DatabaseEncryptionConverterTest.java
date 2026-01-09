package br.com.rentafit.common.security;

import br.com.rentafit.common.util.BeanUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseEncryptionConverterTest {

    @Mock
    private AesCryptoService aesCryptoService;

    private DatabaseEncryptionConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DatabaseEncryptionConverter();
    }

    @Test
    @DisplayName("Should encrypt value when converting to database column")
    void convertToDatabaseColumn() {
        try (MockedStatic<BeanUtil> mockedBeanUtil = mockStatic(BeanUtil.class)) {
            mockedBeanUtil.when(() -> BeanUtil.getBean(AesCryptoService.class)).thenReturn(aesCryptoService);
            when(aesCryptoService.encrypt("plain")).thenReturn("encrypted");

            String result = converter.convertToDatabaseColumn("plain");

            assertThat(result).isEqualTo("encrypted");
        }
    }

    @Test
    @DisplayName("Should decrypt value when converting to entity attribute")
    void convertToEntityAttribute() {
        try (MockedStatic<BeanUtil> mockedBeanUtil = mockStatic(BeanUtil.class)) {
            mockedBeanUtil.when(() -> BeanUtil.getBean(AesCryptoService.class)).thenReturn(aesCryptoService);
            when(aesCryptoService.decrypt("encrypted")).thenReturn("plain");

            String result = converter.convertToEntityAttribute("encrypted");

            assertThat(result).isEqualTo("plain");
        }
    }

    @Test
    @DisplayName("Should return null for null inputs")
    void convert_null() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}

