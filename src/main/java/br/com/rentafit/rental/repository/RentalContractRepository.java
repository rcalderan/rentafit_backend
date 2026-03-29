package br.com.rentafit.rental.repository;

import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalContractRepository extends JpaRepository<RentalContract, UUID> {

    Optional<RentalContract> findByLegacyId(String legacyId);

    Page<RentalContract> findByCustomerId(UUID customerId, Pageable pageable);

    List<RentalContract> findByCustomerIdAndStatus(UUID customerId, ContractStatus status);
}

