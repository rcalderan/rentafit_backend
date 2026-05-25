package br.com.rentafit.people.repository;

import br.com.rentafit.people.domain.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Override
    @EntityGraph(value = "Customer.withAddress", type = EntityGraph.EntityGraphType.FETCH)
    Optional<Customer> findById(UUID id);

    @Override
    @EntityGraph(value = "Customer.withAddress", type = EntityGraph.EntityGraphType.FETCH)
    Page<Customer> findAll(Pageable pageable);

    // BUG-2026-05-04-6: EntityGraph FETCH + derived query pode não aplicar o filtro WHERE
    // corretamente em algumas versões do Hibernate. Usando @Query explícita como fix.
    @EntityGraph(value = "Customer.withAddress", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER('%' || :name || '%')")
    Page<Customer> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    @EntityGraph(value = "Customer.withAddress", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER(:namePrefix || '%')")
    Page<Customer> findByNamePrefixIgnoreCase(@Param("namePrefix") String namePrefix, Pageable pageable);

    Optional<Customer> findByDocument(String document);

    // Query by inherited Person.legacyId field
    @Query("SELECT c FROM Customer c WHERE c.legacyId = :legacyId")
    Optional<Customer> findByLegacyId(@Param("legacyId") Integer legacyId);

    @Query("SELECT MAX(c.legacyId) FROM Customer c")
    Integer findMaxLegacyId();
}
