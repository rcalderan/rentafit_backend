package br.com.rentafit.common.mail;

/**
 * Plain-text e-mail. The body may carry single-use links, so it is never included in toString/logs.
 */
public record OutgoingMail(String to, String subject, String body) {

    @Override
    public String toString() {
        return "OutgoingMail[to=" + maskedRecipient() + ", subject=" + subject + ", body=REDACTED]";
    }

    /** "ana.souza@ex.com" → "a***@ex.com"; used in logs and API responses. */
    public String maskedRecipient() {
        return maskEmail(to);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }
}
