package br.com.rentafit.sector.repository;

import br.com.rentafit.sector.domain.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectorRepository extends JpaRepository<Sector, UUID> {

    List<Sector> findAllByOrderByIssuerCnpjAscNameAsc();

    List<Sector> findByActiveTrueOrderByIssuerCnpjAscNameAsc();

    List<Sector> findByIssuerCnpjOrderByNameAsc(String issuerCnpj);

    Optional<Sector> findFirstByIssuerCnpjAndIsDefaultTrue(String issuerCnpj);

    boolean existsByIssuerCnpjAndNameIgnoreCase(String issuerCnpj, String name);

    boolean existsByIssuerCnpjAndCodeIgnoreCase(String issuerCnpj, String code);
}
