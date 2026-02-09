package br.com.rentafit.product.domain.enums;

public enum ProductStatus {
    AVAILABLE("AVAILABLE"),
    RENTED("RENTED"),
    MAINTENANCE("MAINTENANCE"),
    RESERVED("RESERVED"),
    DAMAGED("DAMAGED"),
    RETIRED("RETIRED"),
    INACTIVE("INACTIVE");

    private final String displayName;

    ProductStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
