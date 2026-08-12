package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.repository.FiscalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FiscalDocumentContentServiceTest {

    @Mock
    private FiscalDocumentRepository repository;

    @InjectMocks
    private FiscalDocumentContentService contentService;

    @Test
    void getAuthorizedXml_retornaXmlAutorizado() {
        UUID id = UUID.randomUUID();
        FiscalDocument document = new FiscalDocument();
        document.setAuthorizedXml("<NFSe/>");
        when(repository.findById(id)).thenReturn(Optional.of(document));

        assertThat(contentService.getAuthorizedXml(id)).isEqualTo("<NFSe/>");
    }

    @Test
    void getAuthorizedXml_rejeitaDocumentoSemXmlAutorizado() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new FiscalDocument()));

        assertThatThrownBy(() -> contentService.getAuthorizedXml(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("XML autorizado");
    }
}
