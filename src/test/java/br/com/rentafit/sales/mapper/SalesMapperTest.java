package br.com.rentafit.sales.mapper;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.repository.FiscalDocumentRepository;
import br.com.rentafit.sales.domain.SalesOrder;
import br.com.rentafit.sales.domain.enums.InvoiceStatus;
import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.SalesOrderDetailsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesMapper - conversão de pedido/DTO")
class SalesMapperTest {

    @Mock
    FiscalDocumentRepository fiscalDocumentRepository;

    @Test
    @DisplayName("toDetailsDTO() mapeia documento fiscal vinculado à venda")
    void toDetailsDTO_mapeiaDocumentoFiscalVinculado() {
        SalesMapper mapper = new SalesMapper(fiscalDocumentRepository);
        UUID orderId = UUID.randomUUID();
        SalesOrder order = order(orderId);
        FiscalDocument doc = fiscalDocument(orderId);
        when(fiscalDocumentRepository.findByOriginAndOriginId(FiscalOrigin.SALES, orderId))
                .thenReturn(List.of(doc));

        SalesOrderDetailsDTO result = mapper.toDetailsDTO(order, List.of());

        assertThat(result.invoiceId()).isEqualTo(doc.getId().toString());
        assertThat(result.invoiceNumber()).isEqualTo(doc.getNumber().toString());
        assertThat(result.invoiceSeries()).isEqualTo(doc.getSeries());
        assertThat(result.invoiceAccessKey()).isEqualTo(doc.getAccessKey());
        assertThat(result.invoiceProtocol()).isEqualTo(doc.getProtocol());
        assertThat(result.invoiceStatus()).isEqualTo("EMITTED");
        assertThat(result.invoiceCustomerEmail()).isEqualTo(doc.getCustomerEmail());
    }

    @Test
    @DisplayName("toDetailsDTO() mantém status do pedido quando não há documento fiscal")
    void toDetailsDTO_semDocumentoFiscal_mantemStatusDoPedido() {
        SalesMapper mapper = new SalesMapper(fiscalDocumentRepository);
        SalesOrder order = order(UUID.randomUUID());
        when(fiscalDocumentRepository.findByOriginAndOriginId(FiscalOrigin.SALES, order.getId()))
                .thenReturn(List.of());

        SalesOrderDetailsDTO result = mapper.toDetailsDTO(order, List.of());

        assertThat(result.invoiceStatus()).isEqualTo(InvoiceStatus.NONE.name());
        assertThat(result.invoiceId()).isEqualTo(order.getInvoiceId());
    }

    private SalesOrder order(UUID id) {
        return SalesOrder.builder()
                .id(id)
                .status(SalesOrderStatus.DRAFT)
                .invoiceStatus(InvoiceStatus.NONE)
                .discountValue(BigDecimal.ZERO)
                .items(List.of())
                .payments(List.of())
                .build();
    }

    private FiscalDocument fiscalDocument(UUID orderId) {
        return FiscalDocument.builder()
                .id(UUID.randomUUID())
                .type(FiscalDocumentType.NFE)
                .status(FiscalDocumentStatus.AUTHORIZED)
                .number(761224818L)
                .series("001")
                .accessKey("35260800000000000000550010000000011000000010")
                .protocol("135260000000001")
                .issueDate(OffsetDateTime.now())
                .totalValue(BigDecimal.valueOf(150.00))
                .origin(FiscalOrigin.SALES)
                .originId(orderId)
                .customerEmail("cliente@exemplo.com")
                .build();
    }
}
