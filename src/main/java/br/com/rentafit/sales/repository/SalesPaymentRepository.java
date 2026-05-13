package br.com.rentafit.sales.repository;

import br.com.rentafit.sales.domain.SalesPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SalesPaymentRepository extends JpaRepository<SalesPayment, UUID> {

    List<SalesPayment> findBySalesOrderIdOrderByInstallmentNumber(UUID salesOrderId);
}
