package br.com.rentafit.rental.repository;

import br.com.rentafit.rental.domain.RentalContractItemMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RentalContractItemMetaRepository extends JpaRepository<RentalContractItemMeta, UUID> {
}
