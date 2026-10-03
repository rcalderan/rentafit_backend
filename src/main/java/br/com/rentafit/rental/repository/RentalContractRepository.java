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

    Page<RentalContract> findByCustomerId(UUID customerId, Pageable pageable);

    List<RentalContract> findByCustomerIdAndStatus(UUID customerId, ContractStatus status);

    @Query("SELECT MAX(r.legacyId) FROM RentalContract r WHERE r.legacyId LIKE :prefix || '%'")
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

