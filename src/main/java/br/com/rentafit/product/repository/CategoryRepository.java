package br.com.rentafit.product.repository;

import br.com.rentafit.product.domain.Category;
import br.com.rentafit.product.domain.enums.ProductTypeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByName(String name);
    List<Category> findByProductType(ProductTypeCategory productType);
    List<Category> findByActiveTrue();

    // Pré-resolve categorias que casam com o FTS — os IDs viram param IN na busca
    // de produtos, permitindo BitmapOr(GIN do vetor + btree de category_id).
    // OR direto sobre join não usaria índice (filtro aplicado após o join).
    @Query("""
            SELECT cat.id FROM Category cat
            WHERE function('fts_match',
                    function('to_tsvector', 'pt_unaccent', coalesce(cat.displayName, '')),
                    function('to_tsquery', 'pt_unaccent', :tsQuery)) = true
            """)
    List<UUID> findIdsMatchingTsQuery(@Param("tsQuery") String tsQuery);
}
