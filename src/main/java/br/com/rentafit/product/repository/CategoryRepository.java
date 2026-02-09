package br.com.rentafit.product.repository;

import br.com.rentafit.product.domain.Category;
import br.com.rentafit.product.domain.enums.ProductTypeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByName(String name);
    List<Category> findByProductType(ProductTypeCategory productType);
    List<Category> findByActiveTrue();
}
