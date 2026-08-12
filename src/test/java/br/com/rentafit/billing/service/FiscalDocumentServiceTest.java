package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.dto.FiscalDocumentSyncRequest;
import br.com.rentafit.billing.repository.FiscalDocumentRepository;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.repository.CustomerRepository;
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
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FiscalDocumentService - CRUD e consultas")
class FiscalDocumentServiceTest {

    @Mock
    FiscalDocumentRepository repository;

    @Mock
    CustomerRepository customerRepository;

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

    @Test
    @DisplayName("saveFromSync() persiste documento fiscal emitido externamente")
    void saveFromSync_persisteDocumentoExterno() {
        FiscalDocumentSyncRequest request = new FiscalDocumentSyncRequest(
                "NFE", "SALES", UUID.randomUUID(), "12345678901234567890123456789012345678901234",
                123L, "1", "123456789012345", "AUTHORIZED", BigDecimal.valueOf(200),
                "Cliente Teste", "12345678901", "teste@example.com",
                OffsetDateTime.now(), "<xml/>", null, null, null, null);
        FiscalDocument doc = documentNfe();
        when(repository.findByAccessKey(request.accessKey())).thenReturn(Optional.empty());
        when(repository.save(any(FiscalDocument.class))).thenReturn(doc);

        FiscalDocument result = service.saveFromSync(request);

        assertThat(result).isSameAs(doc);
        verify(repository).save(any(FiscalDocument.class));
    }

    @Test
    @DisplayName("saveFromSync() usa MANUAL quando origin não é informado")
    void saveFromSync_originNulo_usaManual() {
        FiscalDocumentSyncRequest request = new FiscalDocumentSyncRequest(
                "NFE", null, UUID.randomUUID(), "12345678901234567890123456789012345678901234",
                123L, "1", "123456789012345", "AUTHORIZED", BigDecimal.valueOf(200),
                "Cliente Teste", "12345678901", "teste@example.com",
                OffsetDateTime.now(), "<xml/>", null, null, null, null);
        when(repository.findByAccessKey(request.accessKey())).thenReturn(Optional.empty());
        when(repository.save(any(FiscalDocument.class))).thenReturn(documentNfe());

        service.saveFromSync(request);

        var captor = forClass(FiscalDocument.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOrigin()).isEqualTo(FiscalOrigin.MANUAL);
    }

    @Test
    @DisplayName("saveFromSync() atualiza documento existente pelo accessKey")
    void saveFromSync_atualizaExistente() {
        FiscalDocument existing = documentNfe();
        FiscalDocumentSyncRequest request = new FiscalDocumentSyncRequest(
                "NFE", "SALES", UUID.randomUUID(), existing.getAccessKey(),
                existing.getNumber(), "1", "135260007307799", "CANCELLED", existing.getTotalValue(),
                null, null, null,
                existing.getIssueDate(), existing.getAuthorizedXml(), null, "Erro de teste",
                OffsetDateTime.now(), "135260007307800");
        when(repository.findByAccessKey(existing.getAccessKey())).thenReturn(Optional.of(existing));
        when(repository.save(any(FiscalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FiscalDocument result = service.saveFromSync(request);

        assertThat(result.getStatus()).isEqualTo(FiscalDocumentStatus.CANCELLED);
        assertThat(result.getCancelReason()).isEqualTo("Erro de teste");
        assertThat(result.getCancelProtocol()).isEqualTo("135260007307800");
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

    private FiscalDocument documentNfe() {
        return FiscalDocument.builder()
                .id(UUID.randomUUID())
                .type(FiscalDocumentType.NFE)
                .status(FiscalDocumentStatus.AUTHORIZED)
                .accessKey("12345678901234567890123456789012345678901234")
                .origin(FiscalOrigin.SALES)
                .originId(UUID.randomUUID())
                .issueDate(OffsetDateTime.now())
                .totalValue(BigDecimal.valueOf(200))
                .build();
    }
}
