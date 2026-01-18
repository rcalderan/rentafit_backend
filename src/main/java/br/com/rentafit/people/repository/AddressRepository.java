package br.com.rentafit.people.repository;

import br.com.rentafit.people.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    /**
     * Find addresses by ZIP code
     * @param zipCode normalized ZIP code (8 digits)
     * @return List of addresses found
     */
    List<Address> findByZipCode(String zipCode);

    /**
     * Find address by composite key to avoid duplicates
     */
    Optional<Address> findByZipCodeAndStreetAndCityAndState(String zipCode, String street, String city, String state);
}
