package br.com.rentafit.billing.repository;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.TaxInfo;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("FiscalDocumentRepository - persistência JPA")
class FiscalDocumentRepositoryTest {

    @Autowired
    FiscalDocumentRepository repository;

    @Test
    @DisplayName("Salva e recupera por id")
    void salvaERecuperaPorId() {
        FiscalDocument doc = documentNfse("NFSE-CHAVE-001");
        FiscalDocument saved = repository.save(doc);

        Optional<FiscalDocument> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAccessKey()).isEqualTo("NFSE-CHAVE-001");
        assertThat(found.get().getType()).isEqualTo(FiscalDocumentType.NFSE);
    }

    @Test
    @DisplayName("findByAccessKey retorna documento existente")
    void findByAccessKey_retornaDocumento() {
        repository.save(documentNfse("CHAVE-UNICA-42"));

        Optional<FiscalDocument> found = repository.findByAccessKey("CHAVE-UNICA-42");

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(FiscalDocumentStatus.AUTHORIZED);
    }

    @Test
    @DisplayName("findByAccessKey retorna vazio quando não existe")
    void findByAccessKey_retornaVazio_naoExiste() {
        Optional<FiscalDocument> found = repository.findByAccessKey("INEXISTENTE");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByTypeAndOriginId retorna apenas documentos do type+originId informados")
    void findByTypeAndOriginId_retornaLista() {
        UUID targetOriginId = UUID.randomUUID();
        UUID otherOriginId = UUID.randomUUID();
        repository.save(documentNfseComOrigin("CHAVE-A", FiscalOrigin.SALES, targetOriginId));
        repository.save(documentNfseComOrigin("CHAVE-B", FiscalOrigin.SALES, targetOriginId));
        // CHAVE-C tem originId diferente — não deve aparecer
        repository.save(documentNfseComOrigin("CHAVE-C", FiscalOrigin.SALES, otherOriginId));

        List<FiscalDocument> result = repository.findByTypeAndOriginId(FiscalDocumentType.NFSE, targetOriginId);

        assertThat(result).hasSize(2)
                .allMatch(d -> d.getType() == FiscalDocumentType.NFSE)
                .allMatch(d -> d.getOriginId().equals(targetOriginId));
    }

    @Test
    @DisplayName("findByOriginAndOriginId retorna todos do pedido")
    void findByOriginAndOriginId_retornaLista() {
        UUID orderId = UUID.randomUUID();
        repository.save(documentNfseComOrigin("CHAVE-X", FiscalOrigin.SALES, orderId));
        repository.save(documentNfseComOrigin("CHAVE-Y", FiscalOrigin.SALES, orderId));

        List<FiscalDocument> result = repository.findByOriginAndOriginId(FiscalOrigin.SALES, orderId);

        assertThat(result).hasSize(2);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private FiscalDocument documentNfse(String accessKey) {
        return FiscalDocument.builder()
                .type(FiscalDocumentType.NFSE)
                .status(FiscalDocumentStatus.AUTHORIZED)
                .accessKey(accessKey)
                .origin(FiscalOrigin.SALES)
                .originId(UUID.randomUUID())
                .issueDate(OffsetDateTime.now())
                .totalValue(BigDecimal.valueOf(500))
                .taxes(TaxInfo.builder()
                        .ibsRate(BigDecimal.valueOf(0.025))
                        .ibsValue(BigDecimal.valueOf(12.50))
                        .cbsRate(BigDecimal.valueOf(0.015))
                        .cbsValue(BigDecimal.valueOf(7.50))
                        .isqnRate(BigDecimal.ZERO)
                        .isqnValue(BigDecimal.ZERO)
                        .totalTaxValue(BigDecimal.valueOf(20))
                        .build())
                .build();
    }

    private FiscalDocument documentNfseComOrigin(String accessKey, FiscalOrigin origin, UUID originId) {
        return FiscalDocument.builder()
                .type(FiscalDocumentType.NFSE)
                .status(FiscalDocumentStatus.AUTHORIZED)
                .accessKey(accessKey)
                .origin(origin)
                .originId(originId)
                .issueDate(OffsetDateTime.now())
                .totalValue(BigDecimal.valueOf(100))
                .taxes(TaxInfo.builder()
                        .ibsRate(BigDecimal.ZERO).ibsValue(BigDecimal.ZERO)
                        .cbsRate(BigDecimal.ZERO).cbsValue(BigDecimal.ZERO)
                        .isqnRate(BigDecimal.ZERO).isqnValue(BigDecimal.ZERO)
                        .totalTaxValue(BigDecimal.ZERO)
                        .build())
                .build();
    }
}
