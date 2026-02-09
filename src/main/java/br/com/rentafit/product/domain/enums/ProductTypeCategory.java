package br.com.rentafit.product.domain.enums;

public enum ProductTypeCategory {
    RENTAL("RENTAL"),
    RETAIL("RETAIL"),
    ACCESSORY("ACCESSORY");

    private final String displayName;

    ProductTypeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
