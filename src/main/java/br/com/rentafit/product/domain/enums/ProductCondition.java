package br.com.rentafit.product.domain.enums;

public enum ProductCondition {
    NEW("NEW"),
    EXCELLENT("EXCELLENT"),
    GOOD("GOOD"),
    FAIR("FAIR"),
    POOR("POOR");

    private final String displayName;

    ProductCondition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
