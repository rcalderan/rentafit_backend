package br.com.rentafit.billing.repository.saocarlos;

import br.com.rentafit.billing.domain.saocarlos.SaoCarlosLoteRps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaoCarlosLoteRpsRepository extends JpaRepository<SaoCarlosLoteRps, UUID> {

    Optional<SaoCarlosLoteRps> findByProtocolo(String protocolo);

    Optional<SaoCarlosLoteRps> findByNumeroLote(String numeroLote);
}
