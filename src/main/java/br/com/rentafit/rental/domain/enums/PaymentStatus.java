package br.com.rentafit.rental.domain.enums;

/**
 * Status de um pagamento (parcela). Código legado alinhado com PaymentStatus do frontend.
 */
public enum PaymentStatus {
    PENDING(0, "Pendente"),
    PAID(1, "Pago"),
    CANCELLED(2, "Cancelado"),
    MULTA(3, "Multa por atraso");

    private final int legacyCode;
    private final String description;

    PaymentStatus(int legacyCode, String description) {
        this.legacyCode = legacyCode;
        this.description = description;
    }

    public int getLegacyCode() { return legacyCode; }
    public String getDescription() { return description; }

    public static PaymentStatus fromLegacyCode(int code) {
        for (PaymentStatus s : values()) {
            if (s.legacyCode == code) return s;
        }
        throw new IllegalArgumentException("Unknown PaymentStatus code: " + code);
    }
}

