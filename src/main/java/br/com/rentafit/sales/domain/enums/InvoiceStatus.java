package br.com.rentafit.sales.domain.enums;

/**
 * Status da emissão de NFS-e vinculada ao pedido de venda.
 */
public enum InvoiceStatus {
    NONE("Sem emissão"),
    PENDING_EMISSION("Pendente de emissão"),
    EMITTED("Emitida");

    private final String description;

    InvoiceStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
