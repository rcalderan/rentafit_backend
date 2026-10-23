package br.com.rentafit.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a new password against {@link PasswordRules}. Combine with @NotBlank when mandatory.
 *
 * Usage: @StrongPassword String newPassword
 */
@Documented
@Constraint(validatedBy = RuleBackedValidator.Password.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Senha fraca: 10 a 64 caracteres com minúscula, maiúscula, número e caractere especial";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
