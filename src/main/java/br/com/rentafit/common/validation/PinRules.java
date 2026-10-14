package br.com.rentafit.common.validation;

import java.util.Optional;
import java.util.Set;

/**
 * 4-digit PIN policy. The PIN authorizes operators at the counter, so trivially guessable values are rejected.
 *
 * <p>Usage: {@code PinRules.violation("4831")} returns {@code Optional.empty()}.</p>
 */
public final class PinRules {

    private static final Set<String> COMMON_PINS = Set.of(
            "1212", "2580", "0852", "1004", "2000", "2001", "1122", "6969", "1313", "1010", "2020", "0101",
            "1221", "2112", "1984", "1999", "1969", "4200", "1230", "2468", "1357", "5683", "0007", "9876");

    private PinRules() {
    }

    public static Optional<String> violation(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            return Optional.of("PIN deve ter exatamente 4 dígitos numéricos (recebido: "
                    + (pin == null ? "nulo" : pin.length() + " caracteres") + ")");
        }
        if (allSameDigit(pin)) return Optional.of("PIN não pode repetir o mesmo dígito (ex.: 0000, 1111)");
        if (isSequence(pin)) return Optional.of("PIN não pode ser uma sequência (ex.: 1234, 4321)");
        if (COMMON_PINS.contains(pin)) return Optional.of("PIN muito comum e fácil de adivinhar; escolha outro");
        return Optional.empty();
    }

    private static boolean allSameDigit(String pin) {
        return pin.chars().distinct().count() == 1;
    }

    private static boolean isSequence(String pin) {
        return hasConstantStep(pin, 1) || hasConstantStep(pin, -1);
    }

    private static boolean hasConstantStep(String pin, int step) {
        for (int i = 1; i < pin.length(); i++) {
            if (pin.charAt(i) - pin.charAt(i - 1) != step) return false;
        }
        return true;
    }
}
