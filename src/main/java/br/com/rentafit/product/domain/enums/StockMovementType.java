package br.com.rentafit.product.domain.enums;

public enum StockMovementType {
    ENTRADA("Entrada"),
    SAIDA("Saída"),
    RESERVA("Reserva"),
    LIBERACAO("Liberação"),
    AJUSTE("Ajuste"),
    PERDA("Perda");

    private final String displayName;

    StockMovementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
