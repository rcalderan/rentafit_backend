package br.com.rentafit.rental.domain.enums;

import java.util.List;

/**
 * Status do contrato de locação.
 * O código legado (legacyCode) mantém compatibilidade com o banco de dados MongoDB (noivabd.contrato.situacao).
 */
public enum ContractStatus {
    DRAFT(0, "Proposta"),
    SIGNED(1, "Assinado"),
    FINALIZED(2, "Contrato fechado"),
    REVISION(3, "Revisão"),
    SUPERSEDED(4, "Substituído"),
    CLOSED(5, "Concluído");

    /**
     * Status em que o contrato efetivamente reserva os itens.
     * Regra única compartilhada pelo ItemConflictChecker (bloqueio) e pela
     * consulta de reservas ativas por item (endpoint byItem).
     */
    public static final List<ContractStatus> RESERVATION_STATUSES = List.of(SIGNED, FINALIZED);

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

