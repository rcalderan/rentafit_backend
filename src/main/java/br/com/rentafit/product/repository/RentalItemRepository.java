package br.com.rentafit.product.repository;

import br.com.rentafit.product.domain.RentalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalItemRepository extends JpaRepository<RentalItem, UUID> {
    Optional<RentalItem> findByLegacyId(String legacyId);
    
    @Query("SELECT MAX(r.legacyId) FROM RentalItem r WHERE r.legacyId LIKE :prefix || '%'")
    Optional<String> findMaxLegacyIdByPrefix(@Param("prefix") String prefix);
}
