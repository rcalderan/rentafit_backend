package br.com.rentafit.common.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Password strength policy shared by first-access setup and password change.
 * Messages never echo the password itself — only its measurable properties (length/bytes).
 *
 * <p>Usage: {@code PasswordRules.violation("Rentafit#2026x")} returns {@code Optional.empty()}.</p>
 */
public final class PasswordRules {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 64;
    /** BCrypt silently ignores everything after 72 bytes, so longer inputs would give a false sense of strength. */
    public static final int MAX_BCRYPT_BYTES = 72;

    private static final Pattern LOWERCASE = Pattern.compile("\\p{Ll}");
    private static final Pattern UPPERCASE = Pattern.compile("\\p{Lu}");
    private static final Pattern DIGIT = Pattern.compile("\\p{Nd}");
    private static final Pattern SPECIAL = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");
    private static final Pattern TRIPLE_REPEAT = Pattern.compile("(.)\\1\\1");
    private static final String BLOCKLIST_RESOURCE = "/security/common-passwords.txt";
    private static final Set<String> COMMON_PASSWORDS = loadBlocklist();

    private PasswordRules() {
    }

    public static Optional<String> violation(String password) {
        if (password == null) {
            return Optional.of("Senha é obrigatória (esperado: 10 a 64 caracteres)");
        }
        return firstPresent(sizeViolation(password), compositionViolation(password), patternViolation(password));
    }

    /**
     * Rules that need account context: the password must not embed the username or the e-mail local part.
     */
    public static Optional<String> contextViolation(String password, String username, String email) {
        String lowered = password.toLowerCase(Locale.ROOT);
        if (containsIdentifier(lowered, username)) {
            return Optional.of("Senha não pode conter o username '" + username + "'");
        }
        String emailLocalPart = email == null ? null : email.split("@")[0];
        if (containsIdentifier(lowered, emailLocalPart)) {
            return Optional.of("Senha não pode conter a parte do e-mail antes do '@' ('" + emailLocalPart + "')");
        }
        return Optional.empty();
    }

    private static Optional<String> sizeViolation(String password) {
        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            return Optional.of("Senha deve ter entre 10 e 64 caracteres (recebido: " + length + ")");
        }
        int bytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_BCRYPT_BYTES) {
            return Optional.of("Senha excede 72 bytes UTF-8 (recebido: " + bytes + "); use menos acentos ou emojis");
        }
        return Optional.empty();
    }

    private static Optional<String> compositionViolation(String password) {
        if (!LOWERCASE.matcher(password).find()) return Optional.of("Senha deve conter ao menos 1 letra minúscula");
        if (!UPPERCASE.matcher(password).find()) return Optional.of("Senha deve conter ao menos 1 letra maiúscula");
        if (!DIGIT.matcher(password).find()) return Optional.of("Senha deve conter ao menos 1 número");
        if (!SPECIAL.matcher(password).find()) return Optional.of("Senha deve conter ao menos 1 caractere especial (ex.: @ # $ ! %)");
        return Optional.empty();
    }

    private static Optional<String> patternViolation(String password) {
        if (!password.equals(password.strip())) return Optional.of("Senha não pode começar nem terminar com espaço");
        if (CONTROL.matcher(password).find()) return Optional.of("Senha contém caractere de controle não permitido");
        if (TRIPLE_REPEAT.matcher(password).find()) return Optional.of("Senha não pode repetir o mesmo caractere 3 vezes seguidas");
        if (COMMON_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            return Optional.of("Senha muito comum e fácil de adivinhar; escolha outra");
        }
        return Optional.empty();
    }

    private static boolean containsIdentifier(String loweredPassword, String identifier) {
        return identifier != null && identifier.length() >= UsernameRules.MIN_LENGTH
                && loweredPassword.contains(identifier.toLowerCase(Locale.ROOT));
    }

    @SafeVarargs
    private static Optional<String> firstPresent(Optional<String>... candidates) {
        for (Optional<String> candidate : candidates) {
            if (candidate.isPresent()) return candidate;
        }
        return Optional.empty();
    }

    private static Set<String> loadBlocklist() {
        try (InputStream stream = PasswordRules.class.getResourceAsStream(BLOCKLIST_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Blocklist de senhas ausente no classpath: " + BLOCKLIST_RESOURCE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return reader.lines().map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler blocklist de senhas " + BLOCKLIST_RESOURCE, e);
        }
    }
}
