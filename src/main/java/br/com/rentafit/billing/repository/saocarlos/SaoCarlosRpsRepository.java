package br.com.rentafit.billing.repository.saocarlos;

import br.com.rentafit.billing.domain.saocarlos.SaoCarlosRps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaoCarlosRpsRepository extends JpaRepository<SaoCarlosRps, UUID> {

    Optional<SaoCarlosRps> findByNumeroRpsAndSerieRps(Long numeroRps, String serieRps);

    boolean existsByNumeroRpsAndSerieRps(Long numeroRps, String serieRps);
}
