package br.com.rentafit.billing.mapper;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.dto.FiscalDocumentDetailResponse;
import br.com.rentafit.billing.dto.FiscalDocumentSummaryResponse;
import br.com.rentafit.people.domain.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FiscalDocumentResponseMapper - conversão entidade/DTO")
class FiscalDocumentResponseMapperTest {

    private final FiscalDocumentResponseMapper mapper = new FiscalDocumentResponseMapper();

    @Test
    @DisplayName("toSummary() mapeia campos visíveis em listagem sem expor XML")
    void toSummary_mapeiaCamposSemXml() {
        Customer customer = customer();
        FiscalDocument doc = document(customer);

        FiscalDocumentSummaryResponse result = mapper.toSummary(doc);

        assertThat(result.id()).isEqualTo(doc.getId());
        assertThat(result.type()).isEqualTo("NFE");
        assertThat(result.status()).isEqualTo("AUTHORIZED");
        assertThat(result.customerName()).isEqualTo("Maria Souza");
        assertThat(result.value()).isEqualByComparingTo(BigDecimal.valueOf(350));
    }

    @Test
    @DisplayName("toDetail() mapeia cliente, chave, protocolo e campos de cancelamento")
    void toDetail_mapeiaCamposCompletos() {
        Customer customer = customer();
        FiscalDocument doc = document(customer);
        doc.setServiceDescription("Aluguel de vestido");

        FiscalDocumentDetailResponse result = mapper.toDetail(doc);

        assertThat(result.customerName()).isEqualTo("Maria Souza");
        assertThat(result.customerEmail()).isEqualTo("maria@example.com");
        assertThat(result.accessKey()).isEqualTo("35123456789012345678901234567890123456789012");
        assertThat(result.protocol()).isEqualTo("123456789");
        assertThat(result.serviceDescription()).isEqualTo("Aluguel de vestido");
        assertThat(result.cancelReason()).isNull();
    }

    @Test
    @DisplayName("toSummary() retorna null quando documento é nulo")
    void toSummary_nulo_retornaNull() {
        assertThat(mapper.toSummary(null)).isNull();
    }

    @Test
    @DisplayName("toDetail() retorna null quando documento é nulo")
    void toDetail_nulo_retornaNull() {
        assertThat(mapper.toDetail(null)).isNull();
    }

    private FiscalDocument document(Customer customer) {
        return FiscalDocument.builder()
                .id(UUID.randomUUID())
                .type(FiscalDocumentType.NFE)
                .status(FiscalDocumentStatus.AUTHORIZED)
                .number(123456L)
                .series("1")
                .accessKey("35123456789012345678901234567890123456789012")
                .protocol("123456789")
                .issueDate(OffsetDateTime.now())
                .totalValue(BigDecimal.valueOf(350))
                .customer(customer)
                .origin(FiscalOrigin.SALES)
                .originId(UUID.randomUUID())
                .build();
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setName("Maria Souza");
        customer.setEmail("maria@example.com");
        customer.setDocument("123.456.789-00");
        return customer;
    }
}
