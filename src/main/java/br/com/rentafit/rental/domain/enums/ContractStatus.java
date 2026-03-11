package br.com.rentafit.rental.domain.enums;

/**
 * Status do contrato de locação.
 * O código legado (legacyCode) mantém compatibilidade com o banco de dados MongoDB (noivabd.contrato.situacao).
 */
public enum ContractStatus {
    DRAFT(0, "Proposta"),
    SIGNED(1, "Assinado"),
    FINALIZED(2, "Contrato fechado");

    private final int legacyCode;
    private final String description;

    ContractStatus(int legacyCode, String description) {
        this.legacyCode = legacyCode;
        this.description = description;
    }

    public int getLegacyCode() { return legacyCode; }
    public String getDescription() { return description; }

    public static ContractStatus fromLegacyCode(int code) {
        for (ContractStatus s : values()) {
            if (s.legacyCode == code) return s;
        }
        throw new IllegalArgumentException("Unknown ContractStatus code: " + code);
    }
}

