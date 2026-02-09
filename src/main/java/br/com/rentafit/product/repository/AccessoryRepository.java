package br.com.rentafit.product.repository;

import br.com.rentafit.product.domain.Accessory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessoryRepository extends JpaRepository<Accessory, UUID> {
    Optional<Accessory> findByLegacyId(String legacyId);
}
