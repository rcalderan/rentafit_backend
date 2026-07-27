package br.com.rentafit.billing.mapper;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.dto.FiscalDocumentDetailResponse;
import br.com.rentafit.billing.dto.FiscalDocumentSummaryResponse;
import br.com.rentafit.people.domain.Customer;
import org.springframework.stereotype.Component;

/**
 * Converte {@link FiscalDocument} em DTOs de API.
 * Centraliza o mapeamento entre entidade e contrato de resposta da UI.
 */
@Component
public class FiscalDocumentResponseMapper {

    public FiscalDocumentSummaryResponse toSummary(FiscalDocument document) {
        if (document == null) {
            return null;
        }
        Customer customer = document.getCustomer();
        return FiscalDocumentSummaryResponse.builder()
                .id(document.getId())
                .type(document.getType() != null ? document.getType().name() : null)
                .status(document.getStatus() != null ? document.getStatus().name() : null)
                .number(document.getNumber())
                .series(document.getSeries())
                .accessKey(document.getAccessKey())
                .emissionDate(document.getIssueDate())
                .value(document.getTotalValue())
                .customerName(customer != null ? customer.getName() : null)
                .origin(document.getOrigin() != null ? document.getOrigin().name() : null)
                .originId(document.getOriginId())
                .build();
    }

    public FiscalDocumentDetailResponse toDetail(FiscalDocument document) {
        if (document == null) {
            return null;
        }
        Customer customer = document.getCustomer();
        return FiscalDocumentDetailResponse.builder()
                .id(document.getId())
                .type(document.getType() != null ? document.getType().name() : null)
                .status(document.getStatus() != null ? document.getStatus().name() : null)
                .number(document.getNumber())
                .series(document.getSeries())
                .accessKey(document.getAccessKey())
                .emissionDate(document.getIssueDate())
                .protocol(document.getProtocol())
                .value(document.getTotalValue())
                .serviceDescription(document.getServiceDescription())
                .cancelReason(document.getCancelReason())
                .cancelledAt(document.getCancelledAt())
                .cancelProtocol(document.getCancelProtocol())
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerName(customer != null ? customer.getName() : null)
                .origin(document.getOrigin() != null ? document.getOrigin().name() : null)
                .originId(document.getOriginId())
                .build();
    }
}
