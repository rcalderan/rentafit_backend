package br.com.rentafit.sales.adapter;

import br.com.rentafit.product.domain.RetailProduct;
import br.com.rentafit.product.repository.RetailProductRepository;
import br.com.rentafit.product.repository.StockRepository;
import br.com.rentafit.product.service.StockService;
import br.com.rentafit.sales.port.RetailProductPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter que implementa RetailProductPort usando RetailProductRepository e StockService.
 *
 * <p>Ao migrar para microserviço, substituir por cliente HTTP do serviço Product.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetailProductAdapter implements RetailProductPort {

    private final RetailProductRepository retailProductRepository;
    private final StockRepository stockRepository;
    private final StockService stockService;

    @Override
    public Optional<RetailProductSnapshot> findById(UUID productId) {
        return retailProductRepository.findById(productId).map(this::toSnapshot);
    }

    @Override
    public Optional<RetailProductSnapshot> findBySku(String sku) {
        return retailProductRepository.findBySku(sku).map(this::toSnapshot);
    }

    @Override
    public void reserveStock(UUID productId, int quantity, UUID userId) {
        stockService.reserveStock(productId, quantity, userId);
        log.info("Stock reserved via sales: {} units for product {} by user {}", quantity, productId, userId);
    }

    @Override
    public void releaseStock(UUID productId, int quantity, UUID userId) {
        stockService.releaseReservation(productId, quantity, userId);
        log.info("Stock released via sales: {} units for product {} by user {}", quantity, productId, userId);
    }

    /**
     * Saída definitiva por entrega de venda: reserved--, total--.
     * Diferente de StockService.removeStock() que opera sobre available.
     * Aqui o estoque já foi reservado no confirm, então decrementamos reserved.
     */
    @Override
    public void removeStock(UUID productId, int quantity, UUID userId) {
        var stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new br.com.rentafit.common.exception.ValidationException(
                        "Stock not found for product: " + productId));
        if (stock.getQuantityReserved() < quantity) {
            throw new br.com.rentafit.common.exception.ValidationException(
                    "Quantidade reservada insuficiente para saída definitiva: product=" + productId);
        }
        stock.setQuantityReserved(stock.getQuantityReserved() - quantity);
        stock.setQuantityTotal(stock.getQuantityTotal() - quantity);
        stock.setLastMovementDate(java.time.LocalDateTime.now());
        stockRepository.save(stock);
        log.info("Stock removed (delivery) via sales: {} units for product {} by user {}", quantity, productId, userId);
    }

    private RetailProductSnapshot toSnapshot(RetailProduct product) {
        return new RetailProductSnapshot(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory() != null ? product.getCategory().getDisplayName() : null,
                product.getSize(),
                product.getColor(),
                product.getBrand(),
                product.getValue(),
                product.getDescription(),
                product.getWarrantyDays(),
                product.getStock() != null ? product.getStock().getQuantityAvailable() : 0
        );
    }
}
