package br.com.rentafit.product.repository;

import br.com.rentafit.product.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {
    Optional<Stock> findByProductId(UUID productId);

    @Query("SELECT s FROM Stock s WHERE s.quantityAvailable < s.minStockLevel")
    List<Stock> findLowStockItems();

    List<Stock> findByLocation(String location);
}
