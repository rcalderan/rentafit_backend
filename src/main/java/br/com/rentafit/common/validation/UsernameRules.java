package br.com.rentafit.common.validation;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Security rules for login usernames chosen at sign-up.
 * Lowercase-only keeps uniqueness case-insensitive and blocks homoglyph tricks such as "Admin" vs "admin".
 *
 * <p>Usage: {@code UsernameRules.violation("joao.silva")} returns {@code Optional.empty()}.</p>
 */
public final class UsernameRules {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 30;

    private static final Pattern FORMAT = Pattern.compile("^[a-z][a-z0-9._-]{2,29}$");
    private static final Pattern CONSECUTIVE_SEPARATORS = Pattern.compile("[._-]{2}");
    private static final Set<String> RESERVED = Set.of(
            "admin", "administrator", "administrador", "root", "system", "sistema", "suporte", "support",
            "rentafit", "null", "undefined", "api", "operator", "operador", "manager", "gerente",
            "superuser", "sysadmin", "webmaster", "security", "seguranca");

    private UsernameRules() {
    }

    public static String normalize(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

    public static Optional<String> violation(String raw) {
        String username = normalize(raw);
        if (username == null || username.length() < MIN_LENGTH || username.length() > MAX_LENGTH) {
            return Optional.of("Username deve ter entre 3 e 30 caracteres (recebido: '" + raw + "')");
        }
        if (!FORMAT.matcher(username).matches()) {
            return Optional.of("Username '" + raw + "' inválido: comece com letra e use apenas letras minúsculas, "
                    + "números, '.', '_' ou '-'");
        }
        return structureViolation(username);
    }

    private static Optional<String> structureViolation(String username) {
        if (CONSECUTIVE_SEPARATORS.matcher(username).find() || username.matches(".*[._-]$")) {
            return Optional.of("Username '" + username + "' não pode ter separadores seguidos nem terminar com '.', '_' ou '-'");
        }
        if (RESERVED.contains(username)) {
            return Optional.of("Username '" + username + "' é reservado pelo sistema; escolha outro");
        }
        return Optional.empty();
    }
}
