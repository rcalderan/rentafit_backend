package br.com.rentafit.sales.domain.enums;

/**
 * Status do item dentro de um pedido de venda.
 * Ciclo: PENDING → RESERVED → READY → DELIVERED
 */
public enum SalesItemStatus {
    PENDING("Pendente"),
    RESERVED("Reservado"),
    READY("Pronto"),
    DELIVERED("Entregue");

    private final String description;

    SalesItemStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
