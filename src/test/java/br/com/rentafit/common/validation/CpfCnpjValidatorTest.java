package br.com.rentafit.common.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CpfCnpjValidator — covers the Módulo 11 check-digit algorithm
 * for both CPF and CNPJ, including all-same-digit rejection and null/blank tolerance.
 *
 * Regression for BUG-2026-05-10-8: CPF/CNPJ inválido aceito pelo backend.
 */
@DisplayName("CpfCnpjValidator")
class CpfCnpjValidatorTest {

    private CpfCnpjValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CpfCnpjValidator();
    }

    // ── Null / blank ────────────────────────────────────────────────────

    @Test
    @DisplayName("null is valid — caller must use @NotBlank if required")
    void nullIsValid() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("blank string is valid — caller must use @NotBlank if required")
    void blankIsValid() {
        assertThat(validator.isValid("  ", null)).isTrue();
    }

    // ── CPF válidos ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("CPF válidos")
    class ValidCpf {

        @ParameterizedTest(name = "CPF válido: {0}")
        @ValueSource(strings = {
            "71428793860",   // CPF da cliente fictícia da sessão de QA
            "52998224725",
            "11144477735"
        })
        void validCpfs(String cpf) {
            assertThat(validator.isValid(cpf, null)).isTrue();
        }

        @ParameterizedTest(name = "CPF válido com máscara: {0}")
        @ValueSource(strings = {
            "714.287.938-60",
            "529.982.247-25"
        })
        void validCpfsWithMask(String cpf) {
            assertThat(validator.isValid(cpf, null)).isTrue();
        }
    }

    // ── CPF inválidos ────────────────────────────────────────────────────

    @Nested
    @DisplayName("CPF inválidos")
    class InvalidCpf {

        @ParameterizedTest(name = "CPF todos dígitos iguais: {0}")
        @ValueSource(strings = {
            "00000000000", "11111111111", "22222222222", "33333333333",
            "44444444444", "55555555555", "66666666666", "77777777777",
            "88888888888", "99999999999"
        })
        void allSameDigitsCpfIsInvalid(String cpf) {
            assertThat(validator.isValid(cpf, null)).isFalse();
        }

        @ParameterizedTest(name = "CPF com dígito verificador errado: {0}")
        @ValueSource(strings = {
            "12345678901",   // sequência inválida
            "12345678900",   // check digit errado
            "71428793861"    // CPF válido com último dígito trocado
        })
        void wrongCheckDigitCpfIsInvalid(String cpf) {
            assertThat(validator.isValid(cpf, null)).isFalse();
        }

        @Test
        @DisplayName("CPF com menos de 11 dígitos é inválido")
        void tooShortCpfIsInvalid() {
            assertThat(validator.isValid("1234567890", null)).isFalse();
        }

        @Test
        @DisplayName("CPF com mais de 11 dígitos (mas menos que 14) é inválido")
        void wrongLengthCpfIsInvalid() {
            assertThat(validator.isValid("123456789012", null)).isFalse();
        }
    }

    // ── CNPJ válidos ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("CNPJ válidos")
    class ValidCnpj {

        @ParameterizedTest(name = "CNPJ válido: {0}")
        @ValueSource(strings = {
            "11222333000181",
            "60701190000104",
            "33000167000101",
            "07526557000100"
        })
        void validCnpjs(String cnpj) {
            assertThat(validator.isValid(cnpj, null)).isTrue();
        }

        @ParameterizedTest(name = "CNPJ válido com máscara: {0}")
        @ValueSource(strings = {
            "11.222.333/0001-81",
            "60.701.190/0001-04"
        })
        void validCnpjsWithMask(String cnpj) {
            assertThat(validator.isValid(cnpj, null)).isTrue();
        }
    }

    // ── CNPJ inválidos ───────────────────────────────────────────────────

    @Nested
    @DisplayName("CNPJ inválidos")
    class InvalidCnpj {

        @ParameterizedTest(name = "CNPJ todos dígitos iguais: {0}")
        @ValueSource(strings = {
            "00000000000000", "11111111111111", "22222222222222",
            "33333333333333", "44444444444444", "99999999999999"
        })
        void allSameDigitsCnpjIsInvalid(String cnpj) {
            assertThat(validator.isValid(cnpj, null)).isFalse();
        }

        @ParameterizedTest(name = "CNPJ com dígito verificador errado: {0}")
        @ValueSource(strings = {
            "11222333000100",   // zeros no lugar dos check digits
            "60701190000105",   // último dígito trocado
            "11222333000182"    // último dígito trocado
        })
        void wrongCheckDigitCnpjIsInvalid(String cnpj) {
            assertThat(validator.isValid(cnpj, null)).isFalse();
        }
    }
}
