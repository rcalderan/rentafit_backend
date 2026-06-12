package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.dto.InvoiceEmissionRequestDTO;
import br.com.rentafit.billing.dto.InvoiceEmissionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Endpoints de emissão NFS-e Nacional.
 *
 * <p>Exemplo: POST /api/nfse/emit com InvoiceEmissionRequestDTO → 201 com InvoiceEmissionResponseDTO.</p>
 */
@RestController
@RequestMapping("/api/nfse")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "NFS-e", description = "Emissão e consulta de Notas Fiscais de Serviço (NFS-e Nacional)")
public class NfseController {

    private final NfseEmissionService emissionService;

    @PostMapping("/emit")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Emitir NFS-e", description = "Envia o DPS ao Portal Nacional e persiste o documento fiscal")
    public Mono<ResponseEntity<InvoiceEmissionResponseDTO>> emitir(
            @Valid @RequestBody InvoiceEmissionRequestDTO request) {
        log.info("Solicitação de emissão NFS-e recebida para cliente: {}", request.getCustomerId());
        return emissionService.emit(request)
                .map(resp -> ResponseEntity.status(HttpStatus.CREATED).body(resp))
                .onErrorResume(e -> {
                    log.error("Erro na emissão NFS-e: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}
