package br.com.rentafit.billing.repository;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, UUID> {

    Optional<FiscalDocument> findByAccessKey(String accessKey);

    List<FiscalDocument> findByTypeAndOriginId(FiscalDocumentType type, UUID originId);

    List<FiscalDocument> findByOriginAndOriginId(FiscalOrigin origin, UUID originId);
}
