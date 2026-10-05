package br.com.rentafit.auth.repository;

import br.com.rentafit.auth.domain.InstallationState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface InstallationStateRepository extends JpaRepository<InstallationState, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT state FROM InstallationState state WHERE state.id = 1")
    Optional<InstallationState> lockInstallation();
}
