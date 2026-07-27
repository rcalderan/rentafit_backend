package br.com.rentafit.billing.service;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.dto.FiscalDocumentDetailResponse;
import br.com.rentafit.billing.dto.FiscalDocumentSummaryResponse;
import br.com.rentafit.billing.mapper.FiscalDocumentResponseMapper;
import br.com.rentafit.billing.repository.FiscalDocumentRepository;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FiscalDocumentQueryService - consulta paginada e detalhada")
class FiscalDocumentQueryServiceTest {

    @Mock
    FiscalDocumentRepository repository;

    @Mock
    FiscalDocumentResponseMapper mapper;

    @InjectMocks
    FiscalDocumentQueryService service;

    @Test
    @DisplayName("search() retorna página de summaries")
    void search_retornaPagina() {
        FiscalDocument doc = document();
        Page<FiscalDocument> page = new PageImpl<>(List.of(doc), PageRequest.of(0, 20), 1);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.toSummary(doc)).thenReturn(summary(doc));

        Page<FiscalDocumentSummaryResponse> result = service.search(
                FiscalDocumentType.NFE, FiscalOrigin.SALES, FiscalDocumentStatus.AUTHORIZED,
                null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getById() retorna detalhe quando documento existe")
    void getById_encontrado() {
        UUID id = UUID.randomUUID();
        FiscalDocument doc = document();
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        when(mapper.toDetail(doc)).thenReturn(detail(doc));

        FiscalDocumentDetailResponse result = service.getById(id);

        assertThat(result.id()).isEqualTo(doc.getId());
    }

    @Test
    @DisplayName("getById() lança ResourceNotFoundException quando documento não existe")
    void getById_naoEncontrado_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FiscalDocument");
    }

    private FiscalDocument document() {
        return FiscalDocument.builder()
                .id(UUID.randomUUID())
                .type(FiscalDocumentType.NFE)
                .status(FiscalDocumentStatus.AUTHORIZED)
                .issueDate(OffsetDateTime.now())
                .totalValue(BigDecimal.valueOf(100))
                .build();
    }

    private FiscalDocumentSummaryResponse summary(FiscalDocument doc) {
        return FiscalDocumentSummaryResponse.builder()
                .id(doc.getId())
                .type("NFE")
                .status("AUTHORIZED")
                .value(doc.getTotalValue())
                .build();
    }

    private FiscalDocumentDetailResponse detail(FiscalDocument doc) {
        return FiscalDocumentDetailResponse.builder()
                .id(doc.getId())
                .type("NFE")
                .status("AUTHORIZED")
                .value(doc.getTotalValue())
                .build();
    }
}
