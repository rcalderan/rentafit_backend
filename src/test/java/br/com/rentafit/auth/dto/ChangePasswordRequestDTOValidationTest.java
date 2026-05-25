package br.com.rentafit.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChangePasswordRequestDTO - Bean Validation")
class ChangePasswordRequestDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("DTO válido não gera violações")
    void validDtoShouldPass() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("ValidP@ss1");
        Set<ConstraintViolation<ChangePasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("senha nula gera violação")
    void nullPasswordShouldFail() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO(null);
        Set<ConstraintViolation<ChangePasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
    }

    @Test
    @DisplayName("senha em branco gera violação")
    void blankPasswordShouldFail() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("");
        Set<ConstraintViolation<ChangePasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(3);
        assertThat(violations.stream().map(v -> v.getPropertyPath().toString())).allMatch(p -> p.equals("newPassword"));
    }

    @Test
    @DisplayName("senha curta gera violação de tamanho")
    void shortPasswordShouldFail() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("Short1!");
        Set<ConstraintViolation<ChangePasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(2);
        assertThat(violations.stream().map(v -> v.getPropertyPath().toString())).allMatch(p -> p.equals("newPassword"));
        assertThat(violations.stream().map(ConstraintViolation::getMessage)).anyMatch(m -> m.contains("8"));
    }

    @Test
    @DisplayName("senha sem maiúscula gera violação de pattern")
    void passwordWithoutUppercaseShouldFail() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("lowercase1!");
        Set<ConstraintViolation<ChangePasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
        assertThat(violations.iterator().next().getMessage()).contains("uppercase");
    }

    @Test
    @DisplayName("senha sem número gera violação de pattern")
    void passwordWithoutDigitShouldFail() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("NoDigitPass!");
        Set<ConstraintViolation<ChangePasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
        assertThat(violations.iterator().next().getMessage()).contains("number");
    }

    @Test
    @DisplayName("senha sem caractere especial gera violação de pattern")
    void passwordWithoutSpecialCharShouldFail() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("NoSpecial1");
        Set<ConstraintViolation<ChangePasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
        assertThat(violations.iterator().next().getMessage()).contains("special");
    }
}
