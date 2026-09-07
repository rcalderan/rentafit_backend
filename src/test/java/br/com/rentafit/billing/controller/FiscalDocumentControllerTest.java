package br.com.rentafit.billing.controller;

import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.dto.FiscalDocumentDetailResponse;
import br.com.rentafit.billing.dto.FiscalDocumentSummaryResponse;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.billing.service.FiscalDocumentContentService;
import br.com.rentafit.billing.service.FiscalDocumentQueryService;
import br.com.rentafit.billing.service.FiscalDocumentService;
import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.mapper.FiscalDocumentResponseMapper;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("FiscalDocumentController - endpoints de consulta")
class FiscalDocumentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FiscalDocumentQueryService queryService;

    @MockitoBean
    FiscalDocumentContentService contentService;

    @MockitoBean
    FiscalDocumentService fiscalDocumentService;

    @MockitoBean
    FiscalDocumentResponseMapper fiscalDocumentResponseMapper;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    UserAccountRepository userAccountRepository;

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("GET /api/fiscal-documents retorna página filtrada")
    void list_retornaPagina() throws Exception {
        FiscalDocumentSummaryResponse summary = FiscalDocumentSummaryResponse.builder()
                .id(UUID.randomUUID())
                .type("NFE")
                .status("AUTHORIZED")
                .value(BigDecimal.valueOf(350))
                .customerName("Maria Souza")
                .origin("SALES")
                .build();
        Page<FiscalDocumentSummaryResponse> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1);
        when(queryService.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/fiscal-documents")
                        .param("type", "NFE")
                        .param("origin", "SALES")
                        .param("status", "AUTHORIZED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type", is("NFE")))
                .andExpect(jsonPath("$.content[0].customerName", is("Maria Souza")));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("GET /api/fiscal-documents/{id} retorna detalhe")
    void findById_retornaDetalhe() throws Exception {
        UUID id = UUID.randomUUID();
        FiscalDocumentDetailResponse detail = FiscalDocumentDetailResponse.builder()
                .id(id)
                .type("NFSE")
                .status("AUTHORIZED")
                .accessKey("12345678901234567890123456789012345678901234567890")
                .value(BigDecimal.valueOf(200))
                .customerEmail("cliente@example.com")
                .origin("RENTAL")
                .build();
        when(queryService.getById(id)).thenReturn(detail);

        mockMvc.perform(get("/api/fiscal-documents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.type", is("NFSE")))
                .andExpect(jsonPath("$.accessKey", is(detail.accessKey())))
                .andExpect(jsonPath("$.customerEmail", is("cliente@example.com")));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("GET /api/fiscal-documents/{id}/xml retorna XML autorizado")
    void downloadAuthorizedXml_retornaXml() throws Exception {
        UUID id = UUID.randomUUID();
        when(contentService.getAuthorizedXml(id)).thenReturn("<NFSe/> ");

        mockMvc.perform(get("/api/fiscal-documents/{id}/xml", id))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                        .isEqualTo("<NFSe/> "));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("GET /api/fiscal-documents/{id} retorna 404 quando não existe")
    void findById_naoEncontrado_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(queryService.getById(id)).thenThrow(ResourceNotFoundException.forId("FiscalDocument", id));

        mockMvc.perform(get("/api/fiscal-documents/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("GET /api/fiscal-documents rejeita ordenação por campo não permitido")
    void list_ordenacaoNaoPermitida_retorna400() throws Exception {
        mockMvc.perform(get("/api/fiscal-documents")
                        .param("sort", "signedXml,desc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/fiscal-documents requer autenticação")
    void list_semAutenticacao_retorna403() throws Exception {
        mockMvc.perform(get("/api/fiscal-documents"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("POST /api/fiscal-documents sincroniza documento e retorna 201")
    void sync_retorna201() throws Exception {
        UUID id = UUID.randomUUID();
        FiscalDocument doc = new FiscalDocument();
        java.lang.reflect.Field idField = FiscalDocument.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(doc, id);

        FiscalDocumentDetailResponse detail = FiscalDocumentDetailResponse.builder()
                .id(id)
                .type("NFE")
                .status("AUTHORIZED")
                .value(BigDecimal.valueOf(500))
                .origin("SALES")
                .build();

        when(fiscalDocumentService.saveFromSync(any())).thenReturn(doc);
        when(fiscalDocumentResponseMapper.toDetail(doc)).thenReturn(detail);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/fiscal-documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"NFE\",\"origin\":\"SALES\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.type", is("NFE")));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    @DisplayName("GET /api/fiscal-documents aceita ordenação por campo permitido (issueDate)")
    void list_ordenacaoPermitida_retorna200() throws Exception {
        FiscalDocumentSummaryResponse summary = FiscalDocumentSummaryResponse.builder()
                .id(UUID.randomUUID())
                .type("NFE")
                .status("AUTHORIZED")
                .value(BigDecimal.valueOf(100))
                .build();
        Page<FiscalDocumentSummaryResponse> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1);
        when(queryService.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/fiscal-documents")
                        .param("sort", "issueDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }
}
