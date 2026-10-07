package br.com.rentafit.people.repository;

import br.com.rentafit.people.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByLegacyId(Integer legacyId);
    Optional<Employee> findByInitials(String initials);

    @Query("SELECT employee.initials FROM Employee employee WHERE employee.id = :id")
    Optional<String> findInitialsById(@Param("id") UUID id);
}
