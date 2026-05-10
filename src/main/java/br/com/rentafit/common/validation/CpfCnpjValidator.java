package br.com.rentafit.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates CPF (11 digits) and CNPJ (14 digits) using the Módulo 11 check-digit algorithm.
 * Rejects sequences with all identical digits, which pass format checks but are mathematically invalid.
 *
 * Null and blank values are accepted — combine @ValidCpfCnpj with @NotBlank when the field is mandatory.
 */
public class CpfCnpjValidator implements ConstraintValidator<ValidCpfCnpj, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return switch (digits.length()) {
            case 11 -> isValidCpf(digits);
            case 14 -> isValidCnpj(digits);
            default -> false;
        };
    }

    /**
     * CPF Módulo 11 algorithm.
     * Rejects all-same-digit sequences (e.g. "11111111111") before computing check digits.
     */
    private boolean isValidCpf(String cpf) {
        if (allDigitsSame(cpf)) return false;

        int firstDigit  = computeCpfDigit(cpf, 9);
        int secondDigit = computeCpfDigit(cpf, 10);

        return cpf.charAt(9)  == Character.forDigit(firstDigit,  10)
            && cpf.charAt(10) == Character.forDigit(secondDigit, 10);
    }

    private int computeCpfDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (length + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    /**
     * CNPJ Módulo 11 algorithm.
     * Rejects all-same-digit sequences (e.g. "11111111111111") before computing check digits.
     */
    private boolean isValidCnpj(String cnpj) {
        if (allDigitsSame(cnpj)) return false;

        int firstDigit  = computeCnpjDigit(cnpj, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int secondDigit = computeCnpjDigit(cnpj, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});

        return cnpj.charAt(12) == Character.forDigit(firstDigit,  10)
            && cnpj.charAt(13) == Character.forDigit(secondDigit, 10);
    }

    private int computeCnpjDigit(String cnpj, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.getNumericValue(cnpj.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private boolean allDigitsSame(String s) {
        char first = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != first) return false;
        }
        return true;
    }
}
