package br.com.rentafit.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a 4-digit PIN against {@link PinRules}. Combine with @NotBlank when mandatory.
 *
 * Usage: @ValidPin String pin
 */
@Documented
@Constraint(validatedBy = RuleBackedValidator.Pin.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPin {

    String message() default "PIN inválido: 4 dígitos, sem repetições, sequências ou combinações comuns";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
