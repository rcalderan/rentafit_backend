package br.com.rentafit.people.repository;

import br.com.rentafit.people.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {

    /**
     * Find address by ZIP code (primary key)
     * @param zipCode normalized ZIP code (8 digits)
     * @return Optional containing the address if found
     */
    Optional<Address> findByZipCode(String zipCode);
}
