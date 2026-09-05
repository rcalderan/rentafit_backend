package br.com.rentafit.people.util;

/**
 * Normaliza CPF/CNPJ para comparacao: remove caracteres nao numericos.
 */
public final class DocumentUtils {

    private DocumentUtils() {
    }

    public static String normalize(String document) {
        if (document == null) {
            return null;
        }
        return document.replaceAll("\\D", "");
    }
}
