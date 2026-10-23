package br.com.rentafit.auth.exception;

/**
 * Wrong username/password. Carries how many attempts remain before the account is locked,
 * so the login screen can warn the user before the lock happens.
 */
public class InvalidCredentialsException extends RuntimeException {

    private final int remainingAttempts;

    public InvalidCredentialsException(int remainingAttempts) {
        super("Credenciais inválidas; tentativas restantes antes do bloqueio: " + remainingAttempts);
        this.remainingAttempts = remainingAttempts;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}
