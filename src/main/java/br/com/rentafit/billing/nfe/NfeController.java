package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.dto.NfeEmissionRequest;
import br.com.rentafit.billing.dto.NfeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de emissão NF-e (modelo 55) via SEFAZ-SP.
 *
 * <p>Exemplo: POST /api/nfe/emit com NfeEmissionRequest → 201 com NfeResponse.</p>
 */
@RestController
@RequestMapping("/api/nfe")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "NF-e", description = "Emissão de Nota Fiscal Eletrônica (modelo 55) via SEFAZ-SP")
public class NfeController {

    private final NfeEmissionService emissionService;

    @PostMapping("/emit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Emitir NF-e", description = "Transmite NF-e à SEFAZ-SP e persiste o documento fiscal")
    public ResponseEntity<NfeResponse> emitir(@Valid @RequestBody NfeEmissionRequest request) {
        log.info("Requisição de emissão NF-e para cliente: {}", request.getCustomerId());
        NfeResponse response = emissionService.emit(request);
        HttpStatus status = "AUTHORIZED".equals(response.getStatus())
                ? HttpStatus.CREATED
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(response);
    }
}
