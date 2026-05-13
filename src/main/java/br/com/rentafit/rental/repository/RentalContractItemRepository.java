package br.com.rentafit.rental.repository;

import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RentalContractItemRepository extends JpaRepository<RentalContractItem, UUID> {

    List<RentalContractItem> findByContractId(UUID contractId);

    /**
     * Busca itens de contrato que possuem o mesmo rentalItemId e cujo contrato pai tem
     * eventDate no intervalo [startDate, endDate] e status dentro da lista informada.
     *
     * <p>Utilizado pelo ItemConflictChecker para detectar conflitos de reserva.
     * O índice em rental_contracts.event_date (V12) garante performance desta query.</p>
     */
    @Query("SELECT rci FROM RentalContractItem rci " +
           "WHERE rci.rentalItemId = :rentalItemId " +
           "AND rci.contract.status IN :statuses " +
           "AND rci.contract.eventDate BETWEEN :startDate AND :endDate")
    List<RentalContractItem> findConflictCandidates(
            @Param("rentalItemId") UUID rentalItemId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<ContractStatus> statuses
    );
}

