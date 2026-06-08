package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.repository.FiscalDocumentRepository;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FiscalDocumentService - CRUD e consultas")
class FiscalDocumentServiceTest {

    @Mock
    FiscalDocumentRepository repository;

    @InjectMocks
    FiscalDocumentService service;

    @Test
    @DisplayName("save() persiste e retorna o documento")
    void save_persisteERetorna() {
        FiscalDocument doc = documentNfse();
        when(repository.save(doc)).thenReturn(doc);

        FiscalDocument result = service.save(doc);

        assertThat(result).isSameAs(doc);
        verify(repository).save(doc);
    }

    @Test
    @DisplayName("getById() retorna documento quando existe")
    void getById_encontrado() {
        UUID id = UUID.randomUUID();
        FiscalDocument doc = documentNfse();
        when(repository.findById(id)).thenReturn(Optional.of(doc));

        FiscalDocument result = service.getById(id);

        assertThat(result).isSameAs(doc);
    }

    @Test
    @DisplayName("getById() lança ResourceNotFoundException quando não existe")
    void getById_naoEncontrado_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FiscalDocument");
    }

    @Test
    @DisplayName("getByAccessKey() retorna documento quando encontrado")
    void getByAccessKey_retorna() {
        String chave = "CHAVE-50-DIGITOS-TEST";
        FiscalDocument doc = documentNfse();
        when(repository.findByAccessKey(chave)).thenReturn(Optional.of(doc));

        FiscalDocument result = service.getByAccessKey(chave);

        assertThat(result).isSameAs(doc);
    }

    @Test
    @DisplayName("getByAccessKey() lança ResourceNotFoundException quando não encontrado")
    void getByAccessKey_naoEncontrado_throwsResourceNotFound() {
        when(repository.findByAccessKey("INEXISTENTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByAccessKey("INEXISTENTE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("accessKey");
    }

    // ── helper ─────────────────────────────────────────────────────────────────

    private FiscalDocument documentNfse() {
        return FiscalDocument.builder()
                .id(UUID.randomUUID())
                .type(FiscalDocumentType.NFSE)
                .status(FiscalDocumentStatus.AUTHORIZED)
                .accessKey("CHAVE-TEST")
                .origin(FiscalOrigin.SALES)
                .originId(UUID.randomUUID())
                .issueDate(OffsetDateTime.now())
                .totalValue(BigDecimal.valueOf(200))
                .build();
    }
}
