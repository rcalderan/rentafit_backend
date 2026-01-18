package br.com.rentafit.people.util;

import java.util.regex.Pattern;

/**
 * Utility class for Brazilian ZIP code (CEP) normalization and validation
 */
public final class ZipCodeUtils {

    private static final Pattern CEP_PATTERN = Pattern.compile("^[0-9]{5}-?[0-9]{3}$");
    private static final Pattern ONLY_DIGITS = Pattern.compile("\\D");

    private ZipCodeUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Normalizes a ZIP code by removing all non-digit characters
     * @param zipCode the ZIP code to normalize (e.g., "12345-678" or "12345678")
     * @return normalized ZIP code with 8 digits only (e.g., "12345678")
     * @throws IllegalArgumentException if ZIP code is invalid
     */
    public static String normalize(String zipCode) {
        if (zipCode == null || zipCode.isBlank()) {
            return null;
        }

        String cleaned = zipCode.trim();

        if (cleaned.length() == 8 && cleaned.chars().allMatch(Character::isDigit)) {
            return cleaned;
        }

        if (!CEP_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Invalid ZIP code format: " + zipCode);
        }

        return ONLY_DIGITS.matcher(cleaned).replaceAll("");
    }

    /**
     * Formats a normalized ZIP code for display
     * @param normalizedZipCode 8-digit ZIP code
     * @return formatted ZIP code (e.g., "12345-678")
     */
    public static String format(String normalizedZipCode) {
        if (normalizedZipCode == null || normalizedZipCode.length() != 8) {
            return normalizedZipCode;
        }
        return normalizedZipCode.substring(0, 5) + "-" + normalizedZipCode.substring(5);
    }

    /**
     * Validates if a string is a valid Brazilian ZIP code
     * @param zipCode the ZIP code to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String zipCode) {
        if (zipCode == null || zipCode.isBlank()) {
            return false;
        }
        return CEP_PATTERN.matcher(zipCode.trim()).matches();
    }
}

