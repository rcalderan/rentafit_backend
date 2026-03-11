package br.com.rentafit.rental.domain.enums;

/**
 * Tipo de metadado de um item de contrato.
 * ACESSORIO: acessório catalogado (pode ter accessoryId para controle de estoque).
 * OBSERVACAO: texto livre sem vínculo com produto.
 */
public enum ItemMetaType {
    ACESSORIO("Acessório"),
    OBSERVACAO("Observação");

    private final String description;

    ItemMetaType(String description) { this.description = description; }
    public String getDescription() { return description; }
}

