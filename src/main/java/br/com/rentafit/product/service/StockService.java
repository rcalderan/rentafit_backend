package br.com.rentafit.product.service;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.product.domain.Stock;
import br.com.rentafit.product.domain.StockMovement;
import br.com.rentafit.product.domain.enums.StockMovementType;
import br.com.rentafit.product.dto.StockDTO;
import br.com.rentafit.product.dto.StockMovementDTO;
import br.com.rentafit.product.repository.StockRepository;
import br.com.rentafit.product.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;

    public void reserveStock(UUID productId, Integer quantity, UUID userId) {
        Stock stock = stockRepository.findByProductId(productId)
            .orElseThrow(() -> new ValidationException("Stock not found for product"));

        stock.reserve(quantity);
        stockRepository.save(stock);

        recordMovement(stock, StockMovementType.RESERVA, quantity, userId, "Stock reserved");
        log.info("Stock reserved: {} units for product {}", quantity, productId);
    }

    public void releaseReservation(UUID productId, Integer quantity, UUID userId) {
        Stock stock = stockRepository.findByProductId(productId)
            .orElseThrow(() -> new ValidationException("Stock not found for product"));

        stock.release(quantity);
        stockRepository.save(stock);

        recordMovement(stock, StockMovementType.LIBERACAO, quantity, userId, "Reservation released");
        log.info("Reservation released: {} units for product {}", quantity, productId);
    }

    public void addStock(UUID productId, Integer quantity, String notes, UUID userId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new ValidationException("Stock not found for product"));


        stock.addStock(quantity);
        stockRepository.save(stock);

        recordMovement(stock, StockMovementType.ENTRADA, quantity, userId, notes);
        log.info("Stock added: {} units for product {}", quantity, productId);
    }

    public void removeStock(UUID productId, Integer quantity, String notes, UUID userId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new ValidationException("Stock not found for product"));

        stock.removeStock(quantity);
        stockRepository.save(stock);

        recordMovement(stock, StockMovementType.SAIDA, quantity, userId, notes);
        log.info("Stock removed: {} units for product {}", quantity, productId);
    }

    public void adjustStock(UUID productId, Integer newQuantity, String reason, UUID userId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new ValidationException("Stock not found for product"));

        int difference = newQuantity - stock.getQuantityAvailable();
        stock.setQuantityAvailable(newQuantity);
        stock.setQuantityTotal(newQuantity + stock.getQuantityReserved());
        stockRepository.save(stock);

        recordMovement(stock, StockMovementType.AJUSTE, Math.abs(difference), userId, reason);
        log.info("Stock adjusted for product {}: new quantity {}", productId, newQuantity);
    }

    public void registerLoss(UUID productId, Integer quantity, String reason, UUID userId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new ValidationException("Stock not found for product"));

        stock.removeStock(quantity);
        stockRepository.save(stock);

        recordMovement(stock, StockMovementType.PERDA, quantity, userId, reason);
        log.info("Loss registered: {} units for product {}", quantity, productId);
    }

    public StockDTO getStockByProduct(UUID productId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new ValidationException("Stock not found for product"));

        return convertToDTO(stock);
    }

    public List<StockDTO> getLowStockProducts() {
        return stockRepository.findLowStockItems()
            .stream()
            .map(this::convertToDTO)
            .toList();
    }

    public List<StockMovementDTO> getStockMovements(UUID productId) {
        return stockMovementRepository.findByStockProductIdOrderByMovementDateDesc(productId)
                .stream()
                .map(this::convertToMovementDTO)
                .toList();
    }

    private void recordMovement(Stock stock, StockMovementType type, Integer quantity, UUID userId, String notes) {
        StockMovement movement = StockMovement.builder()
            .stock(stock)
            .type(type)
            .quantity(quantity)
            .movementDate(LocalDateTime.now())
            .userId(userId)
            .notes(notes)
            .build();

        stockMovementRepository.save(movement);
    }

    private StockDTO convertToDTO(Stock stock) {
        return StockDTO.builder()
            .productId(stock.getProduct().getId())
            .quantityAvailable(stock.getQuantityAvailable())
            .quantityReserved(stock.getQuantityReserved())
            .quantityTotal(stock.getQuantityTotal())
            .minStockLevel(stock.getMinStockLevel())
            .location(stock.getLocation())
            .lastMovementDate(stock.getLastMovementDate())
            .build();
    }

    private StockMovementDTO convertToMovementDTO(StockMovement movement) {
        return StockMovementDTO.builder()
            .stockId(movement.getStock().getId())
            .type(movement.getType().name())
            .quantity(movement.getQuantity())
            .movementDate(movement.getMovementDate())
            .userId(movement.getUserId())
            .notes(movement.getNotes())
            .build();
    }
}
