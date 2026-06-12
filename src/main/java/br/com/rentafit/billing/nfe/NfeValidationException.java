package br.com.rentafit.billing.nfe;

/**
 * Lançada quando o XML da NF-e falha na validação estrutural/XSD
 * ou quando a resposta da SEFAZ não pôde ser interpretada.
 */
public class NfeValidationException extends RuntimeException {

    public NfeValidationException(String message) {
        super(message);
    }

    public NfeValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
