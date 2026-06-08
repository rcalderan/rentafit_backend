package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.repository.FiscalDocumentRepository;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Responsável apenas por persistência e consulta de {@link FiscalDocument}.
 * Não contém lógica de emissão — cada serviço de emissão (NfseEmissionService, NfeEmissionService) chama este.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalDocumentService {

    private final FiscalDocumentRepository repository;

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
}
