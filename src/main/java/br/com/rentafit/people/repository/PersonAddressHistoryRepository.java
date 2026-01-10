package br.com.rentafit.people.repository;

import br.com.rentafit.people.domain.PersonAddressHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PersonAddressHistoryRepository extends JpaRepository<PersonAddressHistory, UUID> {

    /**
     * Find address history for a person ordered by start date descending
     */
    List<PersonAddressHistory> findByPersonIdOrderByStartDateDesc(UUID personId);

    /**
     * Find all history records for a person
     */
    List<PersonAddressHistory> findAllByPersonId(UUID personId);
}

