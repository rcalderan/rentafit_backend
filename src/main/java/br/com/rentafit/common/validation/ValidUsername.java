package br.com.rentafit.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a sign-up username against {@link UsernameRules}. Combine with @NotBlank when mandatory.
 *
 * Usage: @ValidUsername String username
 */
@Documented
@Constraint(validatedBy = RuleBackedValidator.Username.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {

    String message() default "Username inválido: 3 a 30 caracteres, letras minúsculas, números, '.', '_' ou '-'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
