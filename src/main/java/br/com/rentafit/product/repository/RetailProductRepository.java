package br.com.rentafit.product.repository;

import br.com.rentafit.product.domain.RetailProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RetailProductRepository extends JpaRepository<RetailProduct, UUID> {
    Optional<RetailProduct> findBySku(String sku);

    // product_search_vec (idx_products_fts) + retail_search_vec sku/details
    // (idx_retail_products_fts) + categoryIds pré-resolvidos (BitmapOr com btree).
    @Query("""
            SELECT i FROM RetailProduct i
            WHERE function('fts_match',
                    function('product_search_vec', i.name, i.description, i.size, i.brand, i.color),
                    function('to_tsquery', 'pt_unaccent', :tsQuery)) = true
               OR function('fts_match',
                    function('retail_search_vec', i.sku, i.details),
                    function('to_tsquery', 'pt_unaccent', :tsQuery)) = true
               OR i.category.id IN :categoryIds
            ORDER BY function('fts_rank_boosted',
                    function('product_search_vec', i.name, i.description, i.size, i.brand, i.color) ||
                    function('retail_search_vec', i.sku, i.details),
                    function('to_tsquery', 'pt_unaccent', :tsQuery),
                    function('to_tsquery', 'raw_unaccent', :exactTsQuery)) DESC, i.name ASC
            """)
    Page<RetailProduct> searchByFullText(@Param("tsQuery") String tsQuery,
                                         @Param("exactTsQuery") String exactTsQuery,
                                         @Param("categoryIds") Collection<UUID> categoryIds,
                                         Pageable pageable);
}
