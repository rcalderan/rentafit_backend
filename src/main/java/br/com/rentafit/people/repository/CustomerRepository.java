package br.com.rentafit.people.repository;

import br.com.rentafit.people.domain.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Override
    @EntityGraph(value = "Customer.withAddress", type = EntityGraph.EntityGraphType.FETCH)
    Optional<Customer> findById(UUID id);

    Optional<Customer> findByDocument(String document);
}
