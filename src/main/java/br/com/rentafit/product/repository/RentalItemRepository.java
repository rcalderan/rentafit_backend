package br.com.rentafit.product.repository;

import br.com.rentafit.product.domain.RentalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalItemRepository extends JpaRepository<RentalItem, UUID> {
    Optional<RentalItem> findByLegacyId(Integer legacyId);

    @Query(value = "SELECT pg_advisory_xact_lock(1917463411)", nativeQuery = true)
    void lockLegacyIdGeneration();

    @Query("SELECT MAX(r.legacyId) FROM RentalItem r")
    Optional<Integer> findMaxLegacyId();
}
