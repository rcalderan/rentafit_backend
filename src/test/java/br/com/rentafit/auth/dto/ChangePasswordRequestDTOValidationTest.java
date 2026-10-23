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
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("Vestido#Azul42");
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
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
    }

    @Test
    @DisplayName("senha curta gera violação de tamanho")
    void shortPasswordShouldFail() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("Short1!x");
        Set<ConstraintViolation<ChangePasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
        assertThat(violations.iterator().next().getMessage()).contains("10 e 64").contains("recebido: 8");
    }

    @Test
    @DisplayName("senha sem maiúscula gera violação de pattern")
    void passwordWithoutUppercaseShouldFail() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("lowercase1!x");
        Set<ConstraintViolation<ChangePasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
        assertThat(violations.iterator().next().getMessage()).contains("maiúscula");
    }

    @Test
    @DisplayName("senha sem número gera violação de pattern")
    void passwordWithoutDigitShouldFail() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("NoDigitPass!x");
        Set<ConstraintViolation<ChangePasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
        assertThat(violations.iterator().next().getMessage()).contains("número");
    }

    @Test
    @DisplayName("senha sem caractere especial gera violação de pattern")
    void passwordWithoutSpecialCharShouldFail() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("NoSpecial12x");
        Set<ConstraintViolation<ChangePasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newPassword");
        assertThat(violations.iterator().next().getMessage()).contains("especial");
    }
}
