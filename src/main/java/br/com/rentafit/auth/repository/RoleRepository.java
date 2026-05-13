package br.com.rentafit.auth.repository;

import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRole(RoleName role);
}

