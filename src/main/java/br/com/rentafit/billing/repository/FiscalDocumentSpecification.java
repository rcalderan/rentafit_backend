package br.com.rentafit.billing.repository;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

/**
 * Especificações dinâmicas para filtrar {@link FiscalDocument} sem expor a entidade.
 * Campos do cliente são consultados via join lazy, sem carregar XML.
 */
public final class FiscalDocumentSpecification {

    private FiscalDocumentSpecification() {
    }

    public static Specification<FiscalDocument> withFilters(
            FiscalDocumentType type,
            FiscalOrigin origin,
            FiscalDocumentStatus status,
            String customerDocument,
            String accessKey,
            OffsetDateTime issueDateFrom,
            OffsetDateTime issueDateTo) {

        return Specification.where(ofType(type))
                .and(ofOrigin(origin))
                .and(ofStatus(status))
                .and(ofCustomerDocument(customerDocument))
                .and(ofAccessKey(accessKey))
                .and(issuedBetween(issueDateFrom, issueDateTo));
    }

    private static Specification<FiscalDocument> ofType(FiscalDocumentType type) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    private static Specification<FiscalDocument> ofOrigin(FiscalOrigin origin) {
        if (origin == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("origin"), origin);
    }

    private static Specification<FiscalDocument> ofStatus(FiscalDocumentStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<FiscalDocument> ofCustomerDocument(String customerDocument) {
        if (customerDocument == null || customerDocument.isBlank()) {
            return null;
        }
        String normalized = customerDocument.strip();
        return (root, query, cb) -> cb.equal(root.get("customer").get("document"), normalized);
    }

    private static Specification<FiscalDocument> ofAccessKey(String accessKey) {
        if (accessKey == null || accessKey.isBlank()) {
            return null;
        }
        String normalized = accessKey.strip();
        return (root, query, cb) -> cb.equal(root.get("accessKey"), normalized);
    }

    private static Specification<FiscalDocument> issuedBetween(
            OffsetDateTime issueDateFrom,
            OffsetDateTime issueDateTo) {
        if (issueDateFrom == null && issueDateTo == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (issueDateFrom != null && issueDateTo != null) {
                return cb.between(root.get("issueDate"), issueDateFrom, issueDateTo);
            }
            if (issueDateFrom != null) {
                return cb.greaterThanOrEqualTo(root.get("issueDate"), issueDateFrom);
            }
            return cb.lessThanOrEqualTo(root.get("issueDate"), issueDateTo);
        };
    }
}
