package br.com.rentafit.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SetupCredentialsRequestDTO - Bean Validation")
class SetupCredentialsRequestDTOValidationTest {

    private static final String VALID_PASSWORD = "Vestido#Azul42";
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<ConstraintViolation<SetupCredentialsRequestDTO>> validate(String password, String pin) {
        return validator.validate(new SetupCredentialsRequestDTO(password, pin));
    }

    @Test
    @DisplayName("DTO válido não gera violações")
    void validDtoShouldPass() {
        assertThat(validate(VALID_PASSWORD, "4831")).isEmpty();
    }

    @Test
    @DisplayName("senha nula gera violação apenas de obrigatoriedade")
    void nullPasswordShouldFail() {
        var violations = validate(null, "4831");
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
    }

    @Test
    @DisplayName("senha curta informa o tamanho recebido e o esperado")
    void shortPasswordShouldFail() {
        var violations = validate("Short1!x", "4831");
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("10 e 64").contains("recebido: 8");
    }

    @Test
    @DisplayName("senha sem maiúscula gera violação específica")
    void passwordWithoutUppercaseShouldFail() {
        assertThat(validate("vestido#azul42", "4831").iterator().next().getMessage()).contains("maiúscula");
    }

    @Test
    @DisplayName("senha comum da blocklist é rejeitada")
    void commonPasswordShouldFail() {
        assertThat(validate("Password@123", "4831").iterator().next().getMessage()).contains("comum");
    }

    @Test
    @DisplayName("mensagem de violação nunca ecoa a senha")
    void violationMessageDoesNotEchoPassword() {
        var violations = validate("semmaiuscula#42", "4831");
        assertThat(violations.iterator().next().getMessage()).doesNotContain("semmaiuscula#42");
    }

    @Test
    @DisplayName("PIN nulo gera violação")
    void nullPinShouldFail() {
        var violations = validate(VALID_PASSWORD, null);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("pin");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12345", "12ab", "0000", "1234", "4321", "1212", "2580"})
    @DisplayName("PIN fora do formato, repetido, sequencial ou comum gera violação")
    void weakPinShouldFail(String pin) {
        var violations = validate(VALID_PASSWORD, pin);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("pin");
    }
}
