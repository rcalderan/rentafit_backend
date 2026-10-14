package br.com.rentafit.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordRules")
class PasswordRulesTest {

    @Test
    @DisplayName("aceita senha forte")
    void acceptsStrongPassword() {
        assertThat(PasswordRules.violation("Vestido#Azul42")).isEmpty();
        assertThat(PasswordRules.violation("Ação com espaço 9!")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Curta1!x|10 e 64",
            "vestido#azul42|maiúscula",
            "VESTIDO#AZUL42|minúscula",
            "Vestido#AzulXX|número",
            "VestidoAzul42x|especial",
            "' Vestido#Azul42'|espaço",
            "'Vestido#Azul42 '|espaço",
            "Vestido#Azuuul42|3 vezes",
            "Password@123|comum",
            "Senha@12345|comum"
    })
    @DisplayName("rejeita senha fora da política com mensagem específica")
    void rejectsWeakPasswords(String password, String expectedPart) {
        assertThat(PasswordRules.violation(password)).hasValueSatisfying(message ->
                assertThat(message).contains(expectedPart));
    }

    @Test
    @DisplayName("rejeita senha acima de 64 caracteres")
    void rejectsTooLong() {
        String longPassword = "Aa1!" + "xy".repeat(31);
        assertThat(PasswordRules.violation(longPassword)).hasValueSatisfying(message ->
                assertThat(message).contains("recebido: 66"));
    }

    @Test
    @DisplayName("rejeita senha que excede o limite de 72 bytes do BCrypt")
    void rejectsOverBcryptLimit() {
        String multiByte = "Aa1!" + "ção".repeat(18);
        assertThat(PasswordRules.violation(multiByte)).hasValueSatisfying(message ->
                assertThat(message).contains("72 bytes"));
    }

    @Test
    @DisplayName("rejeita caractere de controle e senha nula")
    void rejectsControlCharacterAndNull() {
        assertThat(PasswordRules.violation("Vestido#Az\u0007ul42")).hasValueSatisfying(message ->
                assertThat(message).contains("controle"));
        assertThat(PasswordRules.violation(null)).isPresent();
    }

    @Test
    @DisplayName("contexto: rejeita senha contendo username ou parte local do e-mail")
    void rejectsPasswordWithIdentifiers() {
        assertThat(PasswordRules.contextViolation("Joao.Silva#2026", "joao.silva", "x@y.com"))
                .hasValueSatisfying(message -> assertThat(message).contains("username"));
        assertThat(PasswordRules.contextViolation("Mariana#2026x", "outro", "mariana@ex.com"))
                .hasValueSatisfying(message -> assertThat(message).contains("e-mail"));
        assertThat(PasswordRules.contextViolation("Vestido#Azul42", "joao.silva", null)).isEmpty();
        assertThat(PasswordRules.contextViolation("Vestido#Azul42", null, "ab@ex.com")).isEmpty();
    }
}
