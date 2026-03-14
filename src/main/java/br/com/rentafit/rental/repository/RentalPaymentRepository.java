package br.com.rentafit.rental.repository;

import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalPaymentRepository extends JpaRepository<RentalPayment, UUID> {

    List<RentalPayment> findByContractIdOrderByInstallmentNumber(UUID contractId);

    Optional<RentalPayment> findByIdAndContractId(UUID id, UUID contractId);

    long countByContractId(UUID contractId);

    long countByContractIdAndStatusNot(UUID contractId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.value), 0) FROM RentalPayment p " +
           "WHERE p.contract.id = :contractId AND p.status IN :statuses")
    BigDecimal sumValueByContractIdAndStatusIn(
            @Param("contractId") UUID contractId,
            @Param("statuses") List<PaymentStatus> statuses
    );
}

