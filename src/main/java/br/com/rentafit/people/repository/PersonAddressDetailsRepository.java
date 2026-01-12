package br.com.rentafit.people.repository;

import br.com.rentafit.people.domain.PersonAddressDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonAddressDetailsRepository extends JpaRepository<PersonAddressDetails, UUID> {

    /**
     * Find current address for a person (where end_date is null)
     */
    Optional<PersonAddressDetails> findByPersonIdAndEndDateIsNull(UUID personId);

    /**
     * Find all address details for a person (including historical)
     */
    List<PersonAddressDetails> findAllByPersonIdOrderByStartDateDesc(UUID personId);

    /**
     * Find by person ID
     */
    Optional<PersonAddressDetails> findByPersonId(UUID personId);
}
