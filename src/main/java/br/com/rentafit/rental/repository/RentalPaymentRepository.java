package br.com.rentafit.rental.repository;

import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalPaymentRepository extends JpaRepository<RentalPayment, UUID> {

    List<RentalPayment> findByContractIdOrderByInstallmentNumber(UUID contractId);

    Optional<RentalPayment> findByIdAndContractId(UUID id, UUID contractId);

    long countByContractId(UUID contractId);

    long countByContractIdAndStatusNot(UUID contractId, PaymentStatus status);

    @Query("SELECT COALESCE(MAX(p.installmentNumber), 0) FROM RentalPayment p " +
           "WHERE p.contract.id = :contractId AND p.status <> 'CANCELLED'")
    int findMaxInstallmentNumberByContractId(@Param("contractId") UUID contractId);

    @Query("SELECT MAX(p.paymentDate) FROM RentalPayment p " +
           "WHERE p.contract.id = :contractId AND p.status <> 'CANCELLED'")
    Optional<LocalDate> findMaxPaymentDateByContractId(@Param("contractId") UUID contractId);

    @Query("SELECT COALESCE(SUM(p.value), 0) FROM RentalPayment p " +
           "WHERE p.contract.id = :contractId AND p.status IN :statuses")
    BigDecimal sumValueByContractIdAndStatusIn(
            @Param("contractId") UUID contractId,
            @Param("statuses") List<PaymentStatus> statuses
    );

    @Query("""
            SELECT p FROM RentalPayment p
            JOIN FETCH p.contract c
            WHERE p.paymentDate BETWEEN :startDate AND :endDate
              AND p.status <> br.com.rentafit.rental.domain.enums.PaymentStatus.CANCELLED
              AND c.status <> br.com.rentafit.rental.domain.enums.ContractStatus.SUPERSEDED
              AND (p.status <> br.com.rentafit.rental.domain.enums.PaymentStatus.PENDING
                   OR c.status NOT IN (
                       br.com.rentafit.rental.domain.enums.ContractStatus.CANCELLED,
                       br.com.rentafit.rental.domain.enums.ContractStatus.DRAFT
                   ))
            ORDER BY p.paymentDate ASC, p.installmentNumber ASC
            """)
    List<RentalPayment> findActiveMovementsBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT DISTINCT EXTRACT(YEAR FROM p.paymentDate) FROM RentalPayment p
            WHERE p.status <> br.com.rentafit.rental.domain.enums.PaymentStatus.CANCELLED
            ORDER BY EXTRACT(YEAR FROM p.paymentDate) DESC
            """)
    List<Integer> findDistinctPaymentYears();
}
