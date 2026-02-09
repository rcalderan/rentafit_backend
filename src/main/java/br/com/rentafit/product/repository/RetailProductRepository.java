package br.com.rentafit.product.repository;

import br.com.rentafit.product.domain.RetailProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RetailProductRepository extends JpaRepository<RetailProduct, UUID> {
    Optional<RetailProduct> findBySku(String sku);
}
