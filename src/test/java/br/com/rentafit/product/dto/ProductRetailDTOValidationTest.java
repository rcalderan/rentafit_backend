package br.com.rentafit.product.dto;

import br.com.rentafit.product.dto.retail.ProductRetailDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de validação do ProductRetailDTO.
 * Garante que apenas os campos realmente obrigatórios são validados.
 * Regressão para BUG-2026-05-10-1 (description com @NotBlank indevido).
 */
@DisplayName("ProductRetailDTO - Bean Validation")
class ProductRetailDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private ProductRetailDTO validBase() {
        return ProductRetailDTO.builder()
                .name("Camisa Polo")
                .categoryId(UUID.randomUUID())
                .size("M")
                .value(new BigDecimal("99.90"))
                .build();
    }

    @Test
    @DisplayName("DTO válido sem description não gera violações — regressão BUG-2026-05-10-1")
    void validDtoWithoutDescriptionShouldPass() {
        Set<ConstraintViolation<ProductRetailDTO>> violations = validator.validate(validBase());

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("DTO válido com description preenchido não gera violações")
    void validDtoWithDescriptionShouldPass() {
        ProductRetailDTO dto = ProductRetailDTO.builder()
                .name("Camisa Polo")
                .categoryId(UUID.randomUUID())
                .size("M")
                .value(new BigDecimal("99.90"))
                .description("Descrição opcional")
                .build();

        Set<ConstraintViolation<ProductRetailDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("name vazio gera violação com mensagem 'Name is required'")
    void blankNameShouldFail() {
        ProductRetailDTO dto = ProductRetailDTO.builder()
                .name("")
                .categoryId(UUID.randomUUID())
                .size("M")
                .value(new BigDecimal("99.90"))
                .build();

        Set<ConstraintViolation<ProductRetailDTO>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Name is required");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    @DisplayName("size vazio gera violação com mensagem 'Size is required'")
    void blankSizeShouldFail() {
        ProductRetailDTO dto = ProductRetailDTO.builder()
                .name("Camisa Polo")
                .categoryId(UUID.randomUUID())
                .size("")
                .value(new BigDecimal("99.90"))
                .build();

        Set<ConstraintViolation<ProductRetailDTO>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Size is required");
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("size");
    }

    @Test
    @DisplayName("categoryId nulo gera violação com mensagem 'Category is required'")
    void nullCategoryIdShouldFail() {
        ProductRetailDTO dto = ProductRetailDTO.builder()
                .name("Camisa Polo")
                .categoryId(null)
                .size("M")
                .value(new BigDecimal("99.90"))
                .build();

        Set<ConstraintViolation<ProductRetailDTO>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Category is required");
    }

    @Test
    @DisplayName("value nulo gera violação com mensagem 'Value is required'")
    void nullValueShouldFail() {
        ProductRetailDTO dto = ProductRetailDTO.builder()
                .name("Camisa Polo")
                .categoryId(UUID.randomUUID())
                .size("M")
                .value(null)
                .build();

        Set<ConstraintViolation<ProductRetailDTO>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Value is required");
    }
}
