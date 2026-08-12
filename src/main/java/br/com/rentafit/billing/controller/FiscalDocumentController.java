package br.com.rentafit.billing.controller;

import br.com.rentafit.billing.domain.enums.FiscalDocumentStatus;
import br.com.rentafit.billing.domain.enums.FiscalDocumentType;
import br.com.rentafit.billing.domain.enums.FiscalOrigin;
import br.com.rentafit.billing.dto.FiscalDocumentDetailResponse;
import br.com.rentafit.billing.dto.FiscalDocumentSummaryResponse;
import br.com.rentafit.billing.dto.FiscalDocumentSyncRequest;
import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.mapper.FiscalDocumentResponseMapper;
import br.com.rentafit.billing.service.FiscalDocumentContentService;
import br.com.rentafit.billing.service.FiscalDocumentQueryService;
import br.com.rentafit.billing.service.FiscalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * API unificada de consulta de documentos fiscais (NF-e e NFS-e).
 */
@RestController
@RequestMapping("/api/fiscal-documents")
@RequiredArgsConstructor
@Tag(name = "Documentos Fiscais", description = "Consulta de NF-e e NFS-e")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
public class FiscalDocumentController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "issueDate", "number", "status", "totalValue", "createdAt"
    );

    private final FiscalDocumentQueryService queryService;
    private final FiscalDocumentContentService contentService;
    private final FiscalDocumentService fiscalDocumentService;
    private final FiscalDocumentResponseMapper mapper;

    @PostMapping
    @Operation(summary = "Sincronizar documento fiscal", description = "Persiste/atualiza um documento fiscal emitido externamente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Documento sincronizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<FiscalDocumentDetailResponse> sync(
            @RequestBody @Valid FiscalDocumentSyncRequest request) {
        FiscalDocument document = fiscalDocumentService.saveFromSync(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDetail(document));
    }

    @GetMapping
    @Operation(summary = "Listar documentos fiscais", description = "Retorna NF-e e NFS-e paginados com filtros opcionais combináveis")
    @ApiResponse(responseCode = "200", description = "Documentos retornados com sucesso")
    public ResponseEntity<Page<FiscalDocumentSummaryResponse>> list(
            @RequestParam(required = false) FiscalDocumentType type,
            @RequestParam(required = false) FiscalOrigin origin,
            @RequestParam(required = false) FiscalDocumentStatus status,
            @RequestParam(required = false) String customerDocument,
            @RequestParam(required = false) String accessKey,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime issueDateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime issueDateTo,
            @Parameter(hidden = true) Pageable pageable) {

        validateSort(pageable);
        return ResponseEntity.ok(queryService.search(type, origin, status, customerDocument, accessKey,
                issueDateFrom, issueDateTo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar documento fiscal por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento encontrado"),
            @ApiResponse(responseCode = "404", description = "Documento não encontrado")
    })
    public ResponseEntity<FiscalDocumentDetailResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getById(id));
    }

    @GetMapping("/{id}/xml")
    @Operation(summary = "Baixar XML autorizado do documento fiscal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "XML autorizado retornado"),
            @ApiResponse(responseCode = "404", description = "Documento não encontrado"),
            @ApiResponse(responseCode = "409", description = "Documento ainda não possui XML autorizado")
    })
    public ResponseEntity<String> downloadAuthorizedXml(@PathVariable UUID id) {
        String xml = contentService.getAuthorizedXml(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fiscal-document-" + id + ".xml\"")
                .body(xml);
    }

    private void validateSort(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return;
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Ordenação não permitida: '" + order.getProperty() + "'. Campos permitidos: "
                                + ALLOWED_SORT_FIELDS.stream().sorted().collect(Collectors.joining(", ")));
            }
        }
    }
}
