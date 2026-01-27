package br.com.rentafit.billing.controller;

import br.com.rentafit.billing.dto.saocarlos.SaoCarlosEmitirNfseRequestDTO;
import br.com.rentafit.billing.dto.saocarlos.SaoCarlosEmitirNfseResponseDTO;
import br.com.rentafit.billing.service.saocarlos.SaoCarlosNfseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/billing/saocarlos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "NFS-e São Carlos", description = "Endpoints para emissão de NFS-e em São Carlos (GINFES v3.01)")
@SecurityRequirement(name = "bearerAuth")
public class SaoCarlosNfseController {

    private final SaoCarlosNfseService saoCarlosNfseService;

    @GetMapping("/teste")
    public Mono<SaoCarlosEmitirNfseResponseDTO> sctest() {
        SaoCarlosEmitirNfseRequestDTO dto = SaoCarlosEmitirNfseRequestDTO.builder().build();

        dto.setCustomerId(java.util.UUID.fromString("d8d98e8b-2788-4917-95a9-53ef056951be"));
        var rpsList = new java.util.ArrayList<br.com.rentafit.billing.dto.saocarlos.SaoCarlosRpsDTO>();
        var rps = br.com.rentafit.billing.dto.saocarlos.SaoCarlosRpsDTO.builder()
                .numero(123456L)
                .serie("A1")
                .tipo(1)
                .dataEmissao(java.time.LocalDateTime.now())
                .naturezaOperacao(1)
                .regimeEspecialTributacao(6)
                .simplesNacional(1)
                .incentivadorCultural(2)
                .valorServicos(new java.math.BigDecimal("1500.00"))
                .aliquota(new java.math.BigDecimal("0.0500"))
                .itemListaServico("01.07")
                .codigoCnae("6201500")
                .discriminacao("Serviços de desenvolvimento de software sob encomenda.")
                .codigoTributacaoMunicipio("1406")
                .codigoMunicipio(3548708)
                .build();
        rpsList.add(rps);
        dto.setRps(rpsList);
        return saoCarlosNfseService.emitirNfse(dto);
    }

    @PostMapping("/emit")
    @ResponseStatus(HttpStatus.ACCEPTED)
    //@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
        summary = "Emitir NFS-e em São Carlos",
        description = "Envia um lote de RPS para o webservice GINFES de São Carlos. " +
                     "Retorna o protocolo de processamento para consulta posterior."
    )
    public Mono<SaoCarlosEmitirNfseResponseDTO> emitirNfse(
            @Valid @RequestBody SaoCarlosEmitirNfseRequestDTO request) {

        log.info("Recebida requisição de emissão de NFS-e São Carlos para cliente: {}", request.getCustomerId());
        return saoCarlosNfseService.emitirNfse(request);
    }

    @GetMapping("/situacao/{protocolo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
        summary = "Consultar situação do lote",
        description = "Consulta a situação de processamento de um lote pelo protocolo. " +
                     "Situações: 1-Não processado, 2-Processado com sucesso, 3-Processado com erro, 4-Cancelado"
    )
    public Mono<SaoCarlosEmitirNfseResponseDTO> consultarSituacao(@PathVariable String protocolo) {
        log.info("Consultando situação do lote: {}", protocolo);
        return saoCarlosNfseService.consultarSituacaoLote(protocolo);
    }

    @GetMapping("/lote/{protocolo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
        summary = "Consultar lote processado",
        description = "Consulta o resultado completo do lote processado, incluindo as NFS-e geradas."
    )
    public Mono<SaoCarlosEmitirNfseResponseDTO> consultarLote(@PathVariable String protocolo) {
        log.info("Consultando lote: {}", protocolo);
        return saoCarlosNfseService.consultarLoteRps(protocolo);
    }
}
