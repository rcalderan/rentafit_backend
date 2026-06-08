package br.com.rentafit.billing.controller;

import br.com.rentafit.billing.domain.FiscalDocument;
import br.com.rentafit.billing.dto.InvoiceEmissionRequestDTO;
import br.com.rentafit.billing.dto.InvoiceEmissionResponseDTO;
import br.com.rentafit.billing.dto.NfseConsultaResponse;
import br.com.rentafit.billing.service.BillingService;
import br.com.rentafit.billing.service.FiscalDocumentService;
import br.com.rentafit.billing.service.NfsePortalService;
import br.com.rentafit.common.security.CertificateAuthentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/billing/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "NFS-e", description = "Endpoints para emissão e gestão de Notas Fiscais de Serviço Eletrônicas")
@SecurityRequirement(name = "bearerAuth")
public class BillingController {

    private final BillingService billingService;
    private final FiscalDocumentService fiscalDocumentService;
    private final NfsePortalService nfsePortalService;

    @PostMapping("/emit")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
        summary = "Emitir NFS-e",
        description = "Emite uma Nota Fiscal de Serviço Eletrônica para um cliente através do Portal Nacional da NFS-e. " +
                     "Requer autenticação JWT (usuário) e opcionalmente certificado digital A1 (mTLS) para identificar o prestador."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "NFS-e emitida com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos na requisição"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "422", description = "Erro de validação no Portal Nacional"),
        @ApiResponse(responseCode = "500", description = "Erro interno no processamento")
    })
    public Mono<ResponseEntity<InvoiceEmissionResponseDTO>> emitirNfse(
            @Valid @RequestBody InvoiceEmissionRequestDTO request) {
        log.info("Recebida requisição de emissão de NFS-e para cliente: {}", request.getCustomerId());

        // Verifica se há autenticação por certificado (mTLS)
        String cnpjCertificado = extractCnpjFromCertificate();

        if (cnpjCertificado != null) {
            log.info("Emissão autorizada com certificado CNPJ: {}***{}",
                    cnpjCertificado.substring(0, 3),
                    cnpjCertificado.substring(cnpjCertificado.length() - 2));
        } else {
            log.info("Emissão sem certificado mTLS - usando configuração padrão do sistema");
        }

        return billingService.emitInvoice(request)
                .map(response -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(response))
                .doOnSuccess(res -> {
                    if (res.getBody() != null) {
                        log.info("NFS-e emitida com sucesso: {}", res.getBody().getAccessKey());
                    }
                })
                .doOnError(err -> log.error("Erro ao emitir NFS-e: {}", err.getMessage(), err));
    }

    /**
     * Extrai CNPJ do certificado digital se presente na autenticação
     */
    private String extractCnpjFromCertificate() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof CertificateAuthentication) {
            CertificateAuthentication certAuth = (CertificateAuthentication) auth;
            return certAuth.getCnpj();
        }
        return null;
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    @Operation(
        summary = "Consultar NFS-e por ID interno",
        description = "Busca uma NFS-e no banco de dados local pelo ID interno"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "NFS-e encontrada"),
        @ApiResponse(responseCode = "404", description = "NFS-e não encontrada")
    })
    public ResponseEntity<FiscalDocument> consultarPorId(
            @Parameter(description = "ID interno da nota fiscal") @PathVariable UUID id) {
        log.info("Consultando NFS-e por ID: {}", id);

        try {
            return ResponseEntity.ok(fiscalDocumentService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/chave/{chaveAcesso}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    @Operation(
        summary = "Consultar NFS-e no Portal Nacional",
        description = "Consulta os detalhes de uma NFS-e autorizada diretamente no Portal Nacional usando a chave de acesso"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "NFS-e encontrada no portal"),
        @ApiResponse(responseCode = "401", description = "Não autorizado (problemas com certificado/token)"),
        @ApiResponse(responseCode = "404", description = "NFS-e não encontrada no portal")
    })
    public Mono<ResponseEntity<NfseConsultaResponse>> consultarNoPortal(
            @Parameter(description = "Chave de acesso da NFS-e (50 caracteres)")
            @PathVariable String chaveAcesso) {
        log.info("Consultando NFS-e no Portal Nacional: {}", chaveAcesso);

        return nfsePortalService.consultarNfse(chaveAcesso)
                .map(ResponseEntity::ok)
                .onErrorResume(err -> {
                    log.error("Erro ao consultar NFS-e no portal: {}", err.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/chave/{chaveAcesso}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    @Operation(
        summary = "Download do PDF (DANFSe)",
        description = "Baixa o Documento Auxiliar da NFS-e em formato PDF diretamente do Portal Nacional"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "NFS-e não encontrada")
    })
    public Mono<ResponseEntity<byte[]>> downloadPdf(
            @Parameter(description = "Chave de acesso da NFS-e")
            @PathVariable String chaveAcesso) {
        log.info("Download de PDF solicitado para NFS-e: {}", chaveAcesso);

        return nfsePortalService.downloadPdf(chaveAcesso)
                .map(pdfBytes -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment",
                            "NFSe_" + chaveAcesso + ".pdf");
                    headers.setContentLength(pdfBytes.length);

                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(pdfBytes);
                })
                .doOnSuccess(res -> log.info("PDF baixado: {} bytes", res.getBody().length))
                .onErrorResume(err -> {
                    log.error("Erro ao baixar PDF: {}", err.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/chave/{chaveAcesso}/xml")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    @Operation(
        summary = "Download do XML legal",
        description = "Baixa o arquivo XML legal da NFS-e diretamente do Portal Nacional"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "XML gerado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "404", description = "NFS-e não encontrada")
    })
    public Mono<ResponseEntity<String>> downloadXml(
            @Parameter(description = "Chave de acesso da NFS-e")
            @PathVariable String chaveAcesso) {
        log.info("Download de XML solicitado para NFS-e: {}", chaveAcesso);

        return nfsePortalService.downloadXml(chaveAcesso)
                .map(xml -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_XML);
                    headers.setContentDispositionFormData("attachment",
                            "NFSe_" + chaveAcesso + ".xml");

                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(xml);
                })
                .doOnSuccess(res -> log.info("XML baixado: {} caracteres", res.getBody().length()))
                .onErrorResume(err -> {
                    log.error("Erro ao baixar XML: {}", err.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/numero/{numeroNota}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
        summary = "Consultar NFS-e por número",
        description = "Busca uma NFS-e no banco de dados local pelo número da nota"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "NFS-e encontrada"),
        @ApiResponse(responseCode = "404", description = "NFS-e não encontrada")
    })
    public ResponseEntity<Void> consultarPorNumero(
            @Parameter(description = "Número da nota fiscal") @PathVariable Long numeroNota) {
        log.info("Endpoint consultarPorNumero obsoleto, use /chave/{chaveAcesso}");
        return ResponseEntity.status(410).build();
    }
}
