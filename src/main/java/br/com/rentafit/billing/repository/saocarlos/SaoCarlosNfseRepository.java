package br.com.rentafit.billing.repository.saocarlos;

import br.com.rentafit.billing.domain.saocarlos.SaoCarlosNfse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaoCarlosNfseRepository extends JpaRepository<SaoCarlosNfse, UUID> {

    Optional<SaoCarlosNfse> findByNumeroNfse(Long numeroNfse);

    Optional<SaoCarlosNfse> findByCodigoVerificacao(String codigoVerificacao);
}
