package br.com.rentafit.billing.controller;

import br.com.rentafit.billing.dto.via.*;
import br.com.rentafit.billing.service.NfseViaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

@RestController
@RequestMapping("/api/billing/via")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "NFS-e Via (Serpro)", description = "Endpoints para integração com a API NFS-e Via (Serpro)")
@SecurityRequirement(name = "bearerAuth")
public class NfseViaController {

    private final NfseViaService nfseViaService;

    @GetMapping("/testeNfse")
    //@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Receber e validar NFS-e Via")
    public Mono<Object> testeNfse() {
        try {
            return nfseViaService.testeNfse();

        } catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/nfsev")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Receber e validar NFS-e Via")
    public Mono<Object> receberNfse(@RequestBody @Valid RecepcaoRequest request) {
        return nfseViaService.receberNfse(request);
    }

    @PostMapping("/cancelamento")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Receber e validar Evento de Cancelamento")
    public Mono<RecepcaoResponse> cancelarNfse(@RequestBody @Valid CancelamentoRequest request) {
        return nfseViaService.cancelarNfse(request);
    }

    @PostMapping("/substituicao")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Receber NFS-e Via substituta e Evento de Cancelamento por Substituição")
    public Mono<RecepcaoResponse> substituirNfse(@RequestBody @Valid SubstituicaoRequest request) {
        return nfseViaService.substituirNfse(request);
    }

    @GetMapping("/aliquota-efetiva/cnpj/{cnpj}/trecho/{codigoTrecho}/data/{dataReferencia}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Recuperar alíquota efetiva por trecho e data")
    public Mono<Object> consultarAliquotaEfetiva(@PathVariable String cnpj,
                                                 @PathVariable String codigoTrecho,
                                                 @PathVariable String dataReferencia) {
        return nfseViaService.consultarAliquotaEfetiva(cnpj, codigoTrecho, dataReferencia);
    }

    @GetMapping("/consulta/protocolo")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Consultar resultado por protocolo")
    public Mono<Object> consultarPorProtocolo(@RequestParam String protocolo) {
        return nfseViaService.consultarPorProtocolo(protocolo);
    }

    @GetMapping("/consulta/chaveacesso")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Consultar resultado por chave de acesso")
    public Mono<Object> consultarPorChaveAcesso(@RequestParam String chaveAcesso) {
        return nfseViaService.consultarPorChaveAcesso(chaveAcesso);
    }
}
