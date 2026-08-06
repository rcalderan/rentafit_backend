package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.dto.FiscalDocumentSyncRequest;
import br.com.rentafit.billing.repository.FiscalDocumentRepository;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Responsável apenas por persistência e consulta de {@link FiscalDocument}.
 * Não contém lógica de emissão — a emissão de NF-e/NFS-e foi delegada ao
 * microsserviço externo costume-rental-nfe, que sincroniza os documentos via
 * {@code POST /api/fiscal-documents} (ver FiscalDocumentController).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalDocumentService {

    private final FiscalDocumentRepository repository;
    private final CustomerRepository customerRepository;

    @Transactional
    public FiscalDocument save(FiscalDocument document) {
        FiscalDocument saved = repository.save(document);
        log.debug("FiscalDocument salvo: id={} type={} status={}", saved.getId(), saved.getType(), saved.getStatus());
        return saved;
    }

    @Transactional(readOnly = true)
    public FiscalDocument getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("FiscalDocument", id));
    }

    @Transactional(readOnly = true)
    public FiscalDocument getByAccessKey(String accessKey) {
        return repository.findByAccessKey(accessKey)
                .orElseThrow(() -> new ResourceNotFoundException("FiscalDocument", "accessKey", accessKey));
    }

    @Transactional
    public FiscalDocument saveFromSync(FiscalDocumentSyncRequest request) {
        Customer customer = null;
        if (request.customerDocument() != null) {
            customer = customerRepository.findByDocument(request.customerDocument()).orElse(null);
        }

        FiscalDocumentStatus status = request.status() != null
                ? FiscalDocumentStatus.valueOf(request.status().toUpperCase())
                : FiscalDocumentStatus.AUTHORIZED;

        FiscalDocument document = FiscalDocument.builder()
                .type(FiscalDocumentType.valueOf(request.type().toUpperCase()))
                .status(status)
                .origin(FiscalOrigin.valueOf(request.origin().toUpperCase()))
                .originId(request.originId())
                .accessKey(request.accessKey())
                .number(request.number())
                .series(request.series())
                .protocol(request.protocol())
                .issueDate(request.issueDate() != null ? request.issueDate() : OffsetDateTime.now())
                .totalValue(request.totalValue())
                .customer(customer)
                .authorizedXml(request.authorizedXml())
                .rejectionReason(request.rejectionReason())
                .cancelReason(request.cancelReason())
                .cancelledAt(request.cancelledAt())
                .cancelProtocol(request.cancelProtocol())
                .build();

        FiscalDocument saved = repository.save(document);
        log.info("FiscalDocument sincronizado: id={} type={} status={} accessKey={}",
                saved.getId(), saved.getType(), saved.getStatus(), saved.getAccessKey());
        return saved;
    }
}
