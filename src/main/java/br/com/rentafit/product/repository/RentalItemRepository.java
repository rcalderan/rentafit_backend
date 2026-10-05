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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT item FROM RentalItem item WHERE item.id IN :ids ORDER BY item.id")
    java.util.List<RentalItem> lockItems(@org.springframework.data.repository.query.Param("ids") java.util.Collection<UUID> ids);

    @Query("SELECT COUNT(item) > 0 FROM RentalContractItem item WHERE item.rentalItemId = :id "
            + "AND item.contract.status = 'FINALIZED' AND item.delivered = false")
    boolean hasPendingReservation(@org.springframework.data.repository.query.Param("id") UUID id);

    @Query(value = "SELECT pg_advisory_xact_lock(1917463411)", nativeQuery = true)
    void lockLegacyIdGeneration();

    @Query("SELECT MAX(r.legacyId) FROM RentalItem r")
    Optional<Integer> findMaxLegacyId();
}
