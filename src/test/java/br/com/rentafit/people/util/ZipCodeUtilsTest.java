package br.com.rentafit.people.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários de ZipCodeUtils.
 * Testa normalização, formatação e validação de CEPs brasileiros.
 */
@DisplayName("ZipCodeUtils - Unit Tests")
class ZipCodeUtilsTest {

    @Test
    @DisplayName("Deve normalizar CEP com hífen")
    void shouldNormalizeZipCodeWithHyphen() {
        // Arrange
        String zipCode = "01310-100";

        // Act
        String normalized = ZipCodeUtils.normalize(zipCode);

        // Assert
        assertThat(normalized).isEqualTo("01310100");
    }

    @Test
    @DisplayName("Deve normalizar CEP sem hífen")
    void shouldNormalizeZipCodeWithoutHyphen() {
        // Arrange
        String zipCode = "01310100";

        // Act
        String normalized = ZipCodeUtils.normalize(zipCode);

        // Assert
        assertThat(normalized).isEqualTo("01310100");
    }

    @Test
    @DisplayName("Deve normalizar CEP com espaços")
    void shouldNormalizeZipCodeWithSpaces() {
        // Arrange
        String zipCode = "  01310-100  ";

        // Act
        String normalized = ZipCodeUtils.normalize(zipCode);

        // Assert
        assertThat(normalized).isEqualTo("01310100");
    }

    @ParameterizedTest
    @ValueSource(strings = {"01310-100", "12345-678", "87654-321", "00000-000", "99999-999"})
    @DisplayName("Deve normalizar múltiplos CEPs válidos")
    void shouldNormalizeMultipleValidZipCodes(String zipCode) {
        // Act
        String normalized = ZipCodeUtils.normalize(zipCode);

        // Assert
        assertThat(normalized).matches("^\\d{8}$"); // Exactly 8 digits
    }

    @Test
    @DisplayName("Deve retornar null para CEP null")
    void shouldReturnNullForNullZipCode() {
        // Act & Assert
        assertThat(ZipCodeUtils.normalize(null)).isNull();
    }

    @Test
    @DisplayName("Deve retornar null para CEP vazio")
    void shouldReturnNullForEmptyZipCode() {
        // Act & Assert
        assertThat(ZipCodeUtils.normalize("")).isNull();
    }

    @Test
    @DisplayName("Deve retornar null para CEP em branco")
    void shouldReturnNullForBlankZipCode() {
        // Act & Assert
        assertThat(ZipCodeUtils.normalize("   ")).isNull();
    }

    @Test
    @DisplayName("Deve lançar exceção para CEP com formato inválido")
    void shouldThrowExceptionForInvalidFormat() {
        // Act & Assert
        assertThatThrownBy(() -> ZipCodeUtils.normalize("123-456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ZIP code format");
    }

    @Test
    @DisplayName("Deve lançar exceção para CEP com letras")
    void shouldThrowExceptionForZipCodeWithLetters() {
        // Act & Assert
        assertThatThrownBy(() -> ZipCodeUtils.normalize("0131A-100"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ZIP code format");
    }

    @Test
    @DisplayName("Deve formatar CEP normalizado com hífen")
    void shouldFormatNormalizedZipCode() {
        // Arrange
        String normalizedZipCode = "01310100";

        // Act
        String formatted = ZipCodeUtils.format(normalizedZipCode);

        // Assert
        assertThat(formatted).isEqualTo("01310-100");
    }

    @Test
    @DisplayName("Deve formatar múltiplos CEPs")
    void shouldFormatMultipleZipCodes() {
        // Act
        String formatted1 = ZipCodeUtils.format("12345678");
        String formatted2 = ZipCodeUtils.format("00000000");
        String formatted3 = ZipCodeUtils.format("99999999");

        // Assert
        assertThat(formatted1).isEqualTo("12345-678");
        assertThat(formatted2).isEqualTo("00000-000");
        assertThat(formatted3).isEqualTo("99999-999");
    }

    @Test
    @DisplayName("Deve retornar original se comprimento inválido ao formatar")
    void shouldReturnOriginalIfInvalidLengthOnFormat() {
        // Arrange
        String invalidZipCode = "123";

        // Act
        String formatted = ZipCodeUtils.format(invalidZipCode);

        // Assert
        assertThat(formatted).isEqualTo("123");
    }

    @Test
    @DisplayName("Deve retornar null se null ao formatar")
    void shouldReturnNullIfNullOnFormat() {
        // Act
        String formatted = ZipCodeUtils.format(null);

        // Assert
        assertThat(formatted).isNull();
    }

    @Test
    @DisplayName("Deve validar CEP com hífen como válido")
    void shouldValidateZipCodeWithHyphenAsValid() {
        // Act
        boolean isValid = ZipCodeUtils.isValid("01310-100");

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve validar CEP sem hífen como válido")
    void shouldValidateZipCodeWithoutHyphenAsValid() {
        // Act
        boolean isValid = ZipCodeUtils.isValid("01310100");

        // Assert
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"01310-100", "12345-678", "00000-000", "99999-999", "01310100", "12345678"})
    @DisplayName("Deve validar múltiplos CEPs válidos")
    void shouldValidateMultipleValidZipCodes(String zipCode) {
        // Act
        boolean isValid = ZipCodeUtils.isValid(zipCode);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve invalidar CEP null")
    void shouldInvalidateNullZipCode() {
        // Act
        boolean isValid = ZipCodeUtils.isValid(null);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar CEP vazio")
    void shouldInvalidateEmptyZipCode() {
        // Act
        boolean isValid = ZipCodeUtils.isValid("");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar CEP em branco")
    void shouldInvalidateBlankZipCode() {
        // Act
        boolean isValid = ZipCodeUtils.isValid("   ");

        // Assert
        assertThat(isValid).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123-456", "1234-56789", "ABCDE-FGH", "01310-10X", "123ABC-DE"})
    @DisplayName("Deve invalidar CEPs com formato inválido")
    void shouldInvalidateInvalidFormats(String zipCode) {
        // Act
        boolean isValid = ZipCodeUtils.isValid(zipCode);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve normalizar e formatar CEP em ciclo completo")
    void shouldNormalizeAndFormatFullCycle() {
        // Arrange
        String originalZipCode = "01310-100";

        // Act
        String normalized = ZipCodeUtils.normalize(originalZipCode);
        String formatted = ZipCodeUtils.format(normalized);

        // Assert
        assertThat(normalized).isEqualTo("01310100");
        assertThat(formatted).isEqualTo("01310-100");
        assertThat(formatted).isEqualTo(originalZipCode);
    }
}

