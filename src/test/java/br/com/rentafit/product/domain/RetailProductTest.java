package br.com.rentafit.product.domain;

import br.com.rentafit.product.domain.enums.ProductTypeCategory;
import br.com.rentafit.product.dto.retail.ProductRetailDetailsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link RetailProduct}, focando no mapeamento null-safe
 * de {@code stock} em {@link RetailProduct#toDTO()}.
 * <p>
 * Regressão do bug: HTTP 500 em {@code GET /api/v1/products/retail} quando um
 * produto cadastrado não possuía linha de estoque no banco.
 */
@DisplayName("RetailProduct - Domain Tests")
class RetailProductTest {

    private Category sampleCategory() {
        return Category.builder()
                .id(UUID.randomUUID())
                .name("CAMISAS_RETAIL")
                .displayName("Camisas Retail")
                .productType(ProductTypeCategory.RETAIL)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("toDTO() deve retornar stock null quando produto não tem estoque (regression: NPE no endpoint /retail)")
    void toDTONullStockShouldReturnNullInsteadOfThrowing() {
        RetailProduct product = RetailProduct.builder()
                .id(UUID.randomUUID())
                .name("Sem Estoque")
                .category(sampleCategory())
                .value(new BigDecimal("99.00"))
                .sku("SKU-NO-STOCK")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertThat(product.getStock()).isNull();

        ProductRetailDetailsDTO dto = product.toDTO();

        assertThat(dto).isNotNull();
        assertThat(dto.stock()).isNull();
        assertThat(dto.sku()).isEqualTo("SKU-NO-STOCK");
    }

    @Test
    @DisplayName("toDTO() deve mapear stock quando presente")
    void toDTOWithStockShouldMapIt() {
        Stock stock = Stock.builder()
                .quantityAvailable(10)
                .quantityReserved(2)
                .quantityTotal(12)
                .minStockLevel(5)
                .build();

        RetailProduct product = RetailProduct.builder()
                .id(UUID.randomUUID())
                .name("Com Estoque")
                .category(sampleCategory())
                .value(new BigDecimal("150.00"))
                .sku("SKU-WITH-STOCK")
                .stock(stock)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProductRetailDetailsDTO dto = product.toDTO();

        assertThat(dto.stock()).isNotNull();
        assertThat(dto.stock().quantityAvailable()).isEqualTo(10);
        assertThat(dto.stock().quantityTotal()).isEqualTo(12);
    }
}
