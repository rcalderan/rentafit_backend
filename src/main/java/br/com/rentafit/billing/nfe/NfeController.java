package br.com.rentafit.billing.nfe;

import br.com.rentafit.billing.dto.NfeEmissionRequest;
import br.com.rentafit.billing.dto.NfeResponse;
import br.com.rentafit.billing.dto.NfeSignedPayloadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final NfeSefazClient sefazClient;

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

    @PostMapping("/signed-payload")
    @Operation(summary = "Gerar payload assinado",
               description = "Constrói e assina o XML da NF-e, retorna o envelope SOAP completo + URL SEFAZ para o frontend fazer o POST diretamente")
    public ResponseEntity<NfeSignedPayloadResponse> getSignedPayload(
            @Valid @RequestBody NfeEmissionRequest request) {
        log.info("Gerando payload assinado de NF-e para cliente: {}", request.getCustomerId());
        NfeSignedPayloadResponse payload = emissionService.buildSignedPayload(request);
        return ResponseEntity.ok(payload);
    }

    @PostMapping(value = "/transmit", produces = MediaType.TEXT_XML_VALUE)
    @Operation(summary = "Proxy de transmissão SEFAZ",
               description = "Recebe o envelope SOAP assinado do frontend e encaminha à SEFAZ via mTLS, contornando CORS do browser")
    public ResponseEntity<String> transmit(@RequestBody String soapEnvelope) {
        log.info("Proxy: encaminhando envelope SOAP à SEFAZ ({} bytes)", soapEnvelope.length());
        String sefazResponse = sefazClient.transmitRaw(soapEnvelope);
        return ResponseEntity.ok(sefazResponse);
    }
}
