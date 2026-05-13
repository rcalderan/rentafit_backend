package br.com.rentafit.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string is a mathematically valid CPF (11 digits) or CNPJ (14 digits).
 * Rejects sequences with all identical digits (e.g. "11111111111").
 * Null and blank values are considered valid — combine with @NotBlank when required.
 *
 * Usage: @ValidCpfCnpj String document
 */
@Documented
@Constraint(validatedBy = CpfCnpjValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCpfCnpj {

    String message() default "Documento inválido: informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
