package br.com.rentafit.auth.exception;

/**
 * Login refused because a self-registered account has not confirmed its e-mail yet.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String username) {
        super("Conta '" + username + "' aguardando confirmação de e-mail");
    }
}
