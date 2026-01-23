package br.com.rentafit.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta da emissão de NFS-e")
public class InvoiceEmissionResponseDTO {

    @Schema(description = "ID interno da nota fiscal")
    private UUID id;

    @Schema(description = "Chave de acesso da NFS-e (50 caracteres)")
    private String accessKey;

    @Schema(description = "Número da nota fiscal")
    private Long invoiceNumber;

    @Schema(description = "Protocolo de processamento no Portal Nacional")
    private String protocol;

    @Schema(description = "Status da nota fiscal")
    private String status;

    @Schema(description = "Data/hora de emissão")
    private OffsetDateTime issueDate;

    @Schema(description = "Data/hora de processamento no portal")
    private OffsetDateTime processingDate;

    @Schema(description = "Valor do serviço")
    private BigDecimal serviceValue;

    @Schema(description = "Informações tributárias")
    private TaxInfoDTO taxes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxInfoDTO {
        private BigDecimal ibsRate;
        private BigDecimal ibsValue;
        private BigDecimal cbsRate;
        private BigDecimal cbsValue;
        private BigDecimal isqnRate;
        private BigDecimal isqnValue;
        private BigDecimal totalTaxValue;
    }
}
