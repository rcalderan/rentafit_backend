package br.com.rentafit.product.repository;

import br.com.rentafit.product.domain.StockMovement;
import br.com.rentafit.product.domain.enums.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findByStockIdOrderByMovementDateDesc(UUID stockId);
    List<StockMovement> findByStockProductIdOrderByMovementDateDesc(UUID productId);
    List<StockMovement> findByMovementDateBetween(LocalDateTime start, LocalDateTime end);
    List<StockMovement> findByType(StockMovementType type);
    List<StockMovement> findByUserId(UUID userId);
}
