package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.dto.FiscalDocumentDetailResponse;
import br.com.rentafit.billing.dto.FiscalDocumentSummaryResponse;
import br.com.rentafit.billing.mapper.FiscalDocumentResponseMapper;
import br.com.rentafit.billing.repository.FiscalDocumentRepository;
import br.com.rentafit.billing.repository.FiscalDocumentSpecification;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Consulta paginada e detalhada de documentos fiscais para a API da UI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalDocumentQueryService {

    private final FiscalDocumentRepository repository;
    private final FiscalDocumentResponseMapper mapper;

    @Transactional(readOnly = true)
    public Page<FiscalDocumentSummaryResponse> search(
            FiscalDocumentType type,
            FiscalOrigin origin,
            FiscalDocumentStatus status,
            String customerDocument,
            String accessKey,
            OffsetDateTime issueDateFrom,
            OffsetDateTime issueDateTo,
            Pageable pageable) {

        return repository.findAll(FiscalDocumentSpecification.withFilters(
                        type, origin, status, customerDocument, accessKey, issueDateFrom, issueDateTo), pageable)
                .map(mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public FiscalDocumentDetailResponse getById(UUID id) {
        FiscalDocument document = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("FiscalDocument", id));
        return mapper.toDetail(document);
    }
}
