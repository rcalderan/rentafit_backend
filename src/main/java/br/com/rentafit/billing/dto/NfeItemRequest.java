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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item (produto) de uma NF-e")
public class NfeItemRequest {

    @NotBlank(message = "Código do produto é obrigatório")
    @Schema(description = "Código interno do produto")
    private String productCode;

    @NotBlank(message = "Descrição do produto é obrigatória")
    @Schema(description = "Descrição do produto")
    private String description;

    @NotBlank(message = "NCM é obrigatório")
    @Schema(description = "Nomenclatura Comum do Mercosul (8 dígitos)")
    private String ncm;

    @NotBlank(message = "CFOP é obrigatório")
    @Schema(description = "Código Fiscal de Operações e Prestações (4 dígitos)")
    private String cfop;

    @NotBlank(message = "Unidade comercial é obrigatória")
    @Schema(description = "Unidade comercial (ex.: UN, KG, CX)")
    private String unit;

    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
    @Schema(description = "Quantidade comercializada")
    private BigDecimal quantity;

    @NotNull(message = "Valor unitário é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor unitário deve ser maior que zero")
    @Schema(description = "Valor unitário comercial")
    private BigDecimal unitValue;

    @Schema(description = "CEST (Código Especificador da Substituição Tributária)")
    private String cest;

    @Schema(description = "Indicador de escala relevante (S/N)", allowableValues = {"S", "N"})
    private String indEscala;
}
