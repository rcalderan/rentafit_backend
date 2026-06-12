package br.com.rentafit.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requisição para emissão de NFS-e")
public class InvoiceEmissionRequestDTO {

    @NotNull(message = "ID do cliente é obrigatório")
    @Schema(description = "ID do cliente (tomador do serviço)")
    private UUID customerId;

    @NotNull(message = "Valor do serviço é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor do serviço deve ser maior que zero")
    @Schema(description = "Valor bruto do serviço prestado")
    private BigDecimal serviceValue;

    @NotBlank(message = "Código NBS é obrigatório")
    @Schema(description = "Código NBS (Nomenclatura Brasileira de Serviços)")
    private String nbsCode;

    @NotBlank(message = "Descrição do serviço é obrigatória")
    @Schema(description = "Descrição detalhada do serviço prestado")
    private String serviceDescription;

    @NotBlank(message = "Código do município de prestação é obrigatório")
    @Schema(description = "Código IBGE do município onde o serviço foi prestado")
    private String cityCode;

    @Schema(description = "Alíquota do IBS (se não informada, será calculada automaticamente)")
    private BigDecimal ibsRate;

    @Schema(description = "Alíquota do CBS (se não informada, será calculada automaticamente)")
    private BigDecimal cbsRate;

    @Schema(description = "Alíquota do ISQN (se aplicável)")
    private BigDecimal isqnRate;

    @Schema(description = "ID da entidade de origem (pedido de venda, contrato, etc.)")
    private UUID originId;

    @Schema(description = "Tipo de origem: SALES, RENTAL, MANUAL")
    private String origin;
}
