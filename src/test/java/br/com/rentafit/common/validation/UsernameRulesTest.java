package br.com.rentafit.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UsernameRules")
class UsernameRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {"joao.silva", "ana_2026", "m-costa", "abc", "  Joao.Silva  ", "a23456789012345678901234567890"})
    @DisplayName("aceita usernames válidos (normalizando caixa e espaços)")
    void acceptsValidUsernames(String username) {
        assertThat(UsernameRules.violation(username)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "ab, 3 e 30",
            "a234567890123456789012345678901, 3 e 30",
            "1joao, comece com letra",
            "joao@mail.com, comece com letra",
            "joão, comece com letra",
            "jo ao, comece com letra",
            "joao..silva, separadores",
            "joao._silva, separadores",
            "joao., separadores",
            "admin, reservado",
            "Root, reservado"
    })
    @DisplayName("rejeita username fora da política com mensagem específica")
    void rejectsInvalidUsernames(String username, String expectedMessagePart) {
        assertThat(UsernameRules.violation(username)).hasValueSatisfying(message ->
                assertThat(message).contains(expectedMessagePart));
    }

    @Test
    @DisplayName("mensagem inclui o valor recebido")
    void messageIncludesOffendingValue() {
        assertThat(UsernameRules.violation("1abc")).hasValueSatisfying(message -> assertThat(message).contains("'1abc'"));
    }

    @Test
    @DisplayName("null é rejeitado e normalize preserva null")
    void handlesNull() {
        assertThat(UsernameRules.violation(null)).isPresent();
        assertThat(UsernameRules.normalize(null)).isNull();
    }
}
