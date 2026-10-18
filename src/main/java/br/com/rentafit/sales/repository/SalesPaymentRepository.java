package br.com.rentafit.sales.repository;

import br.com.rentafit.sales.domain.SalesPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SalesPaymentRepository extends JpaRepository<SalesPayment, UUID> {

    List<SalesPayment> findBySalesOrderIdOrderByInstallmentNumber(UUID salesOrderId);

    @Query("""
            SELECT p FROM SalesPayment p
            JOIN FETCH p.salesOrder o
            WHERE p.paymentDate BETWEEN :startDate AND :endDate
              AND p.status <> br.com.rentafit.rental.domain.enums.PaymentStatus.CANCELLED
              AND (p.status <> br.com.rentafit.rental.domain.enums.PaymentStatus.PENDING
                   OR o.status NOT IN (
                       br.com.rentafit.sales.domain.enums.SalesOrderStatus.CANCELLED,
                       br.com.rentafit.sales.domain.enums.SalesOrderStatus.DRAFT
                   ))
            ORDER BY p.paymentDate ASC, p.installmentNumber ASC
            """)
    List<SalesPayment> findActiveMovementsBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT DISTINCT EXTRACT(YEAR FROM p.paymentDate) FROM SalesPayment p
            WHERE p.status <> br.com.rentafit.rental.domain.enums.PaymentStatus.CANCELLED
            ORDER BY EXTRACT(YEAR FROM p.paymentDate) DESC
            """)
    List<Integer> findDistinctPaymentYears();
}
