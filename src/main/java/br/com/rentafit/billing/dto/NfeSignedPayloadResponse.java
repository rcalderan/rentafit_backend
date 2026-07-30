package br.com.rentafit.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta com o payload SOAP assinado para transmissão direta à SEFAZ
 * (abordagem de teste onde o frontend faz o POST).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload SOAP assinado para transmissão direta à SEFAZ")
public class NfeSignedPayloadResponse {

    @Schema(description = "Envelope SOAP 1.2 completo pronto para POST")
    private String soapEnvelope;

    @Schema(description = "XML da NF-e assinado (sem envelope SOAP)")
    private String signedXml;

    @Schema(description = "URL completa do endpoint SEFAZ")
    private String sefazUrl;

    @Schema(description = "Valor do header Content-Type para o POST")
    private String contentType;

    @Schema(description = "Valor do header SOAPAction para o POST")
    private String soapAction;

    @Schema(description = "Chave de acesso da NF-e (44 dígitos)")
    private String accessKey;
}
