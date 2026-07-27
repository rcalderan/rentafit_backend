package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.repository.FiscalDocumentRepository;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FiscalDocumentContentService {

    private final FiscalDocumentRepository repository;

    @Transactional(readOnly = true)
    public String getAuthorizedXml(UUID documentId) {
        FiscalDocument document = repository.findById(documentId)
                .orElseThrow(() -> ResourceNotFoundException.forId("FiscalDocument", documentId));
        if (document.getAuthorizedXml() == null || document.getAuthorizedXml().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Documento fiscal '" + documentId + "' não possui XML autorizado disponível");
        }
        return document.getAuthorizedXml();
    }
}
