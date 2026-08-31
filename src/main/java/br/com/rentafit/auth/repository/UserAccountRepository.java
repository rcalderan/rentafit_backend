package br.com.rentafit.auth.repository;

import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsername(String username);

    @Query("SELECT ua FROM UserAccount ua WHERE ua.username = :username")
    @EntityGraph(attributePaths = {"person", "roles"})
    Optional<UserAccount> findByUsernameWithDetails(@Param("username") String username);

    @Query("SELECT ua FROM UserAccount ua")
    @EntityGraph(attributePaths = {"person", "roles"})
    Page<UserAccount> findAllWithDetails(Pageable pageable);

    @Query("SELECT DISTINCT ua FROM UserAccount ua JOIN ua.roles role "
            + "WHERE ua.isActive = true AND role.role IN :roles "
            + "AND EXISTS (SELECT employee.id FROM Employee employee WHERE employee.id = ua.id) "
            + "ORDER BY ua.person.name")
    @EntityGraph(attributePaths = {"person", "roles"})
    List<UserAccount> findActiveAttendants(@Param("roles") Set<RoleName> roles);
}

