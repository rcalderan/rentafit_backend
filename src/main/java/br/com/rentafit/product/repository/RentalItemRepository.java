package br.com.rentafit.product.repository;

import br.com.rentafit.product.domain.RentalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalItemRepository extends JpaRepository<RentalItem, UUID> {
    Optional<RentalItem> findByLegacyId(String legacyId);
}
