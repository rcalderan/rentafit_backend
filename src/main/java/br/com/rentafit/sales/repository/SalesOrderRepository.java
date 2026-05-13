package br.com.rentafit.sales.repository;

import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {

    Optional<SalesOrder> findByLegacyId(String legacyId);

    List<SalesOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Page<SalesOrder> findByStatus(SalesOrderStatus status, Pageable pageable);

    @Query("SELECT o FROM SalesOrder o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:dateFrom IS NULL OR o.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR o.createdAt <= :dateTo)")
    Page<SalesOrder> findWithFilters(
            @Param("status") SalesOrderStatus status,
            @Param("dateFrom") OffsetDateTime dateFrom,
            @Param("dateTo") OffsetDateTime dateTo,
            Pageable pageable
    );

    /** Conta pedidos criados hoje para gerar o sequencial V-YYYYMMDD-N. */
    @Query("SELECT COUNT(o) FROM SalesOrder o WHERE o.legacyId LIKE :prefix%")
    long countByLegacyIdPrefix(@Param("prefix") String prefix);
}
