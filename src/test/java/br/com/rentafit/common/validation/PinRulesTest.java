package br.com.rentafit.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PinRules")
class PinRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {"4831", "9071", "3518", "0429"})
    @DisplayName("aceita PINs não triviais")
    void acceptsStrongPins(String pin) {
        assertThat(PinRules.violation(pin)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "123, 4 dígitos",
            "12345, 4 dígitos",
            "12a4, 4 dígitos",
            "0000, repetir",
            "7777, repetir",
            "0123, sequência",
            "6789, sequência",
            "9876, sequência",
            "3210, sequência",
            "1212, comum",
            "2580, comum",
            "1004, comum"
    })
    @DisplayName("rejeita PIN fraco com mensagem específica")
    void rejectsWeakPins(String pin, String expectedPart) {
        assertThat(PinRules.violation(pin)).hasValueSatisfying(message -> assertThat(message).contains(expectedPart));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("PIN nulo informa o motivo")
    void rejectsNull() {
        assertThat(PinRules.violation(null)).hasValueSatisfying(message -> assertThat(message).contains("nulo"));
    }
}
