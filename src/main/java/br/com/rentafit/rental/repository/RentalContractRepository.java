package br.com.rentafit.rental.repository;

import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalContractRepository extends JpaRepository<RentalContract, UUID> {

    Optional<RentalContract> findByLegacyId(String legacyId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT contract FROM RentalContract contract WHERE contract.id = :id")
    Optional<RentalContract> lockById(@Param("id") UUID id);

    @Query(value = "SELECT pg_advisory_xact_lock(1917463412)", nativeQuery = true)
    void lockLegacyIdGeneration();

    Page<RentalContract> findByCustomerId(UUID customerId, Pageable pageable);

    List<RentalContract> findByCustomerIdAndStatus(UUID customerId, ContractStatus status);

    @Query(value = "SELECT legacy_id FROM rental_contracts WHERE legacy_id LIKE :prefix || '%' "
            + "ORDER BY length(legacy_id) DESC, legacy_id DESC LIMIT 1", nativeQuery = true)
    Optional<String> findMaxLegacyIdByPrefix(@Param("prefix") String prefix);

    Optional<RentalContract> findByParentContractIdAndStatusNot(UUID parentContractId, ContractStatus excludeStatus);

    /**
     * Contratos com eventDate exato e status na lista, ordenados por nome do cliente.
     * Itens e metadata são carregados via @Fetch(SUBSELECT) nas entidades (evita N+1
     * sem causar MultipleBagFetchException).
     */
    List<RentalContract> findByEventDateAndStatusInOrderByCustomerNameAsc(
            LocalDate eventDate, List<ContractStatus> statuses);

    /**
     * Contratos cujo eventDate está entre startDate e endDate (ambos inclusivos),
     * com status na lista, ordenados por data do evento e depois por nome do cliente.
     */
    List<RentalContract> findByEventDateBetweenAndStatusInOrderByEventDateAscCustomerNameAsc(
            LocalDate startDate, LocalDate endDate, List<ContractStatus> statuses);

    /**
     * Contratos que reservam um item (status na lista, eventDate a partir de fromDate),
     * ordenados pelo evento mais próximo.
     *
     * <p>DISTINCT porque o mesmo rentalItemId pode aparecer em mais de um item do
     * mesmo contrato. A coleção items não é lida após o join — sem custo de SUBSELECT.</p>
     */
    @Query("""
            SELECT DISTINCT c FROM RentalContract c
            JOIN c.items i
            WHERE i.rentalItemId = :rentalItemId
              AND c.status IN :statuses
              AND c.eventDate >= :fromDate
            ORDER BY c.eventDate ASC
            """)
    List<RentalContract> findReservationsByRentalItemId(
            @Param("rentalItemId") UUID rentalItemId,
            @Param("statuses") List<ContractStatus> statuses,
            @Param("fromDate") LocalDate fromDate);

    /**
     * Agregado de valores por contrato para listagens paginadas.
     *
     * <p>Substitui o carregamento das coleções items/payments via SUBSELECT em findAll:
     * sem restrição na query raiz, o subselect ignorava o LIMIT e varria as tabelas
     * inteiras (~43k itens + ~49k pagamentos) para renderizar uma página de 5 DTOs.</p>
     */
    interface ContractValueTotal {
        UUID getContractId();
        BigDecimal getTotal();
    }

    @Query("""
            SELECT i.contract.id AS contractId, SUM(i.value) AS total
            FROM RentalContractItem i
            WHERE i.contract.id IN :contractIds
            GROUP BY i.contract.id
            """)
    List<ContractValueTotal> sumItemValuesByContractIds(@Param("contractIds") Collection<UUID> contractIds);

    @Query("""
            SELECT p.contract.id AS contractId, SUM(p.value) AS total
            FROM RentalPayment p
            WHERE p.contract.id IN :contractIds AND p.status = 'PAID'
            GROUP BY p.contract.id
            """)
    List<ContractValueTotal> sumPaidValuesByContractIds(@Param("contractIds") Collection<UUID> contractIds);
}

