package br.com.rentafit.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta da SEFAZ para a transmissão de NF-e")
public class NfeResponse {

    @Schema(description = "Chave de acesso da NF-e (44 dígitos)")
    private String accessKey;

    @Schema(description = "Número do protocolo de autorização (nProt)")
    private String protocol;

    @Schema(description = "Código de status SEFAZ (cStat)")
    private String cStat;

    @Schema(description = "Motivo/descrição do status (xMotivo)")
    private String xMotivo;

    @Schema(description = "Status interno: AUTHORIZED, REJECTED")
    private String status;

    @Schema(description = "XML autorizado (nfeProc) quando autorizada")
    private String authorizedXml;
}
