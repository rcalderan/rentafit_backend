package br.com.rentafit.product.repository;

import br.com.rentafit.product.domain.RentalItem;
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
public interface RentalItemRepository extends JpaRepository<RentalItem, UUID> {
    Optional<RentalItem> findByLegacyId(Integer legacyId);

    // product_search_vec cobre name/description/size/brand/color (idx_products_fts);
    // legacy_id tem vetor próprio (idx_rental_items_fts); categoryIds vem pré-resolvido
    // para permitir BitmapOr com o btree de category_id.
    @Query("""
            SELECT i FROM RentalItem i
            WHERE function('fts_match',
                    function('product_search_vec', i.name, i.description, i.size, i.brand, i.color),
                    function('to_tsquery', 'pt_unaccent', :tsQuery)) = true
               OR function('fts_match',
                    function('legacy_id_search_vec', i.legacyId),
                    function('to_tsquery', 'pt_unaccent', :tsQuery)) = true
               OR i.category.id IN :categoryIds
            ORDER BY function('fts_rank_boosted',
                    function('product_search_vec', i.name, i.description, i.size, i.brand, i.color),
                    function('to_tsquery', 'pt_unaccent', :tsQuery),
                    function('to_tsquery', 'raw_unaccent', :exactTsQuery)) DESC, i.name ASC
            """)
    Page<RentalItem> searchByFullText(@Param("tsQuery") String tsQuery,
                                      @Param("exactTsQuery") String exactTsQuery,
                                      @Param("categoryIds") Collection<UUID> categoryIds,
                                      Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT item FROM RentalItem item WHERE item.id IN :ids ORDER BY item.id")
    java.util.List<RentalItem> lockItems(@org.springframework.data.repository.query.Param("ids") java.util.Collection<UUID> ids);

    @Query("SELECT COUNT(item) > 0 FROM RentalContractItem item WHERE item.rentalItemId = :id "
            + "AND item.contract.status = 'FINALIZED' AND item.delivered = false")
    boolean hasPendingReservation(@org.springframework.data.repository.query.Param("id") UUID id);

    @Query(value = "SELECT pg_advisory_xact_lock(1917463411)", nativeQuery = true)
    void lockLegacyIdGeneration();

    @Query("SELECT MAX(r.legacyId) FROM RentalItem r")
    Optional<Integer> findMaxLegacyId();
}
