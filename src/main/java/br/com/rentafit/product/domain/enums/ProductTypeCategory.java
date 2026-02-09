package br.com.rentafit.product.domain.enums;

public enum ProductTypeCategory {
    RENTAL("Aluguel"),
    RETAIL("Venda"),
    ACCESSORY("Acessório");

    private final String displayName;

    ProductTypeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
