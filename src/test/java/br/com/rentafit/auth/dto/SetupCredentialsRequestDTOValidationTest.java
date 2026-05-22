package br.com.rentafit.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SetupCredentialsRequestDTO - Bean Validation")
class SetupCredentialsRequestDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("DTO válido não gera violações")
    void validDtoShouldPass() {
        SetupCredentialsRequestDTO dto = new SetupCredentialsRequestDTO("ValidP@ss1", "1234");
        Set<ConstraintViolation<SetupCredentialsRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("senha nula gera violação")
    void nullPasswordShouldFail() {
        SetupCredentialsRequestDTO dto = new SetupCredentialsRequestDTO(null, "1234");
        Set<ConstraintViolation<SetupCredentialsRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
    }

    @Test
    @DisplayName("senha em branco gera violação")
    void blankPasswordShouldFail() {
        SetupCredentialsRequestDTO dto = new SetupCredentialsRequestDTO("", "1234");
        Set<ConstraintViolation<SetupCredentialsRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(3);
        assertThat(violations.stream().map(v -> v.getPropertyPath().toString())).allMatch(p -> p.equals("newPassword"));
    }

    @Test
    @DisplayName("senha curta gera violação de tamanho")
    void shortPasswordShouldFail() {
        SetupCredentialsRequestDTO dto = new SetupCredentialsRequestDTO("Short1!", "1234");
        Set<ConstraintViolation<SetupCredentialsRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(2);
        assertThat(violations.stream().map(v -> v.getPropertyPath().toString())).allMatch(p -> p.equals("newPassword"));
        assertThat(violations.stream().map(ConstraintViolation::getMessage)).anyMatch(m -> m.contains("8"));
    }

    @Test
    @DisplayName("senha sem maiúscula gera violação de pattern")
    void passwordWithoutUppercaseShouldFail() {
        SetupCredentialsRequestDTO dto = new SetupCredentialsRequestDTO("lowercase1!", "1234");
        Set<ConstraintViolation<SetupCredentialsRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
        assertThat(violations.iterator().next().getMessage()).contains("uppercase");
    }

    @Test
    @DisplayName("senha sem número gera violação de pattern")
    void passwordWithoutDigitShouldFail() {
        SetupCredentialsRequestDTO dto = new SetupCredentialsRequestDTO("NoDigitPass!", "1234");
        Set<ConstraintViolation<SetupCredentialsRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
        assertThat(violations.iterator().next().getMessage()).contains("number");
    }

    @Test
    @DisplayName("senha sem caractere especial gera violação de pattern")
    void passwordWithoutSpecialCharShouldFail() {
        SetupCredentialsRequestDTO dto = new SetupCredentialsRequestDTO("NoSpecial1", "1234");
        Set<ConstraintViolation<SetupCredentialsRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
        assertThat(violations.iterator().next().getMessage()).contains("special");
    }

    @Test
    @DisplayName("PIN nulo gera violação")
    void nullPinShouldFail() {
        SetupCredentialsRequestDTO dto = new SetupCredentialsRequestDTO("ValidP@ss1", null);
        Set<ConstraintViolation<SetupCredentialsRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("pin");
    }

    @Test
    @DisplayName("PIN com menos de 4 dígitos gera violação")
    void shortPinShouldFail() {
        SetupCredentialsRequestDTO dto = new SetupCredentialsRequestDTO("ValidP@ss1", "123");
        Set<ConstraintViolation<SetupCredentialsRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("pin");
        assertThat(violations.iterator().next().getMessage()).contains("4 numeric digits");
    }

    @Test
    @DisplayName("PIN com letras gera violação")
    void alphaPinShouldFail() {
        SetupCredentialsRequestDTO dto = new SetupCredentialsRequestDTO("ValidP@ss1", "12ab");
        Set<ConstraintViolation<SetupCredentialsRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("pin");
    }

    @Test
    @DisplayName("PIN com mais de 4 dígitos gera violação")
    void longPinShouldFail() {
        SetupCredentialsRequestDTO dto = new SetupCredentialsRequestDTO("ValidP@ss1", "12345");
        Set<ConstraintViolation<SetupCredentialsRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("pin");
    }
}
