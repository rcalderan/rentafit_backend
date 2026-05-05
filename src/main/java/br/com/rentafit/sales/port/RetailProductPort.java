package br.com.rentafit.sales.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de acesso ao componente Product (RetailProduct) para o módulo Sales.
 *
 * <p>Abstrai a dependência do componente Sales sobre o componente Product.
 * Ao migrar para microserviço, apenas o adapter é substituído por um cliente HTTP.</p>
 */
public interface RetailProductPort {

    Optional<RetailProductSnapshot> findById(UUID productId);

    Optional<RetailProductSnapshot> findBySku(String sku);

    /** Reserva estoque: available--, reserved++ */
    void reserveStock(UUID productId, int quantity);

    /** Libera reserva: reserved--, available++ */
    void releaseStock(UUID productId, int quantity);

    /** Remove do estoque definitivamente: reserved--, total-- (saída por venda) */
    void removeStock(UUID productId, int quantity);

    /**
     * Snapshot dos dados do produto gravado no item da venda.
     * Desacopla o pedido de alterações futuras no catálogo.
     */
    record RetailProductSnapshot(
            UUID id,
            String sku,
            String name,
            String categoryName,
            String size,
            String color,
            String brand,
            BigDecimal value,
            String description,
            Integer warrantyDays,
            Integer quantityAvailable
    ) {}
}
