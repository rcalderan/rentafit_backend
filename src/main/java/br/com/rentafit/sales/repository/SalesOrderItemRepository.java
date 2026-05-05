package br.com.rentafit.sales.repository;

import br.com.rentafit.sales.domain.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, UUID> {
}
