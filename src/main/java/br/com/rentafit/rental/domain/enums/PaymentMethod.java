package br.com.rentafit.rental.domain.enums;

/**
 * Forma de pagamento. Código legado alinhado com PaymentMethod do frontend.
 */
public enum PaymentMethod {
    CASH(0, "Dinheiro"),
    PIX(1, "PIX"),
    CREDIT_CARD(2, "Cartão de Crédito"),
    DEBIT_CARD(3, "Cartão de Débito"),
    BANK_TRANSFER(4, "Transferência");

    private final int legacyCode;
    private final String label;

    PaymentMethod(int legacyCode, String label) {
        this.legacyCode = legacyCode;
        this.label = label;
    }

    public int getLegacyCode() { return legacyCode; }
    public String getLabel() { return label; }

    public static PaymentMethod fromLegacyCode(int code) {
        for (PaymentMethod m : values()) {
            if (m.legacyCode == code) return m;
        }
        throw new IllegalArgumentException("Unknown PaymentMethod code: " + code);
    }
}

