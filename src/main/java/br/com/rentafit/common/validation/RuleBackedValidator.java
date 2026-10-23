package br.com.rentafit.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Function;

/**
 * Bridges a {@code *Rules} class to Bean Validation, replacing the default message with the specific rule broken.
 * Null and blank are accepted so each field decides with {@code @NotBlank} whether it is mandatory.
 */
public abstract class RuleBackedValidator<A extends Annotation> implements ConstraintValidator<A, String> {

    private final Function<String, Optional<String>> rule;

    protected RuleBackedValidator(Function<String, Optional<String>> rule) {
        this.rule = rule;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        Optional<String> violation = rule.apply(value);
        if (violation.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(escape(violation.get())).addConstraintViolation();
        return false;
    }

    /** Messages may echo user input; escaping prevents EL/placeholder interpolation of that input. */
    private static String escape(String message) {
        return message.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}").replace("$", "\\$");
    }

    public static class Username extends RuleBackedValidator<ValidUsername> {
        public Username() {
            super(UsernameRules::violation);
        }
    }

    public static class Password extends RuleBackedValidator<StrongPassword> {
        public Password() {
            super(PasswordRules::violation);
        }
    }

    public static class Pin extends RuleBackedValidator<ValidPin> {
        public Pin() {
            super(PinRules::violation);
        }
    }
}
