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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("FiscalDocumentSpecification - filtros dinâmicos")
class FiscalDocumentSpecificationTest {

    @Autowired
    FiscalDocumentRepository repository;

    @Test
    @DisplayName("withFilters combina type, origin e status")
    void withFilters_combinaFiltros() {
        repository.save(document(FiscalDocumentType.NFE, FiscalOrigin.SALES, FiscalDocumentStatus.AUTHORIZED));
        repository.save(document(FiscalDocumentType.NFSE, FiscalOrigin.RENTAL, FiscalDocumentStatus.AUTHORIZED));
        repository.save(document(FiscalDocumentType.NFE, FiscalOrigin.SALES, FiscalDocumentStatus.REJECTED));

        Page<FiscalDocument> page = repository.findAll(
                FiscalDocumentSpecification.withFilters(
                        FiscalDocumentType.NFE, FiscalOrigin.SALES, FiscalDocumentStatus.AUTHORIZED,
                        null, null, null, null),
                PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getType()).isEqualTo(FiscalDocumentType.NFE);
    }

    @Test
    @DisplayName("withFilters retorna todos quando nenhum filtro é informado")
    void withFilters_semFiltros_retornaTodos() {
        repository.save(document(FiscalDocumentType.NFE, FiscalOrigin.SALES, FiscalDocumentStatus.AUTHORIZED));
        repository.save(document(FiscalDocumentType.NFSE, FiscalOrigin.RENTAL, FiscalDocumentStatus.AUTHORIZED));

        Page<FiscalDocument> page = repository.findAll(
                FiscalDocumentSpecification.withFilters(null, null, null, null, null, null, null),
                PageRequest.of(0, 20, Sort.by("issueDate").descending()));

        assertThat(page.getContent()).hasSize(2);
    }

    private FiscalDocument document(FiscalDocumentType type, FiscalOrigin origin, FiscalDocumentStatus status) {
        return FiscalDocument.builder()
                .type(type)
                .status(status)
                .origin(origin)
                .originId(UUID.randomUUID())
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
