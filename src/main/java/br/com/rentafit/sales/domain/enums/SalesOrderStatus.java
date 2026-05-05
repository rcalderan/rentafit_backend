package br.com.rentafit.sales.domain.enums;

/**
 * Status do pedido de venda.
 * Ciclo: DRAFT → CONFIRMED → PAID → COMPLETED | CANCELLED
 */
public enum SalesOrderStatus {
    DRAFT("Rascunho"),
    CONFIRMED("Confirmado"),
    PAID("Pago"),
    COMPLETED("Concluído"),
    CANCELLED("Cancelado");

    private final String description;

    SalesOrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
