package br.com.rentafit.billing.dto.saocarlos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RPS (Recibo Provisório de Serviços) para São Carlos")
public class SaoCarlosRpsDTO {

    @NotNull(message = "Número do RPS é obrigatório")
    @Positive
    @Schema(description = "Número do RPS", example = "1")
    private Long numero;

    @NotBlank(message = "Série do RPS é obrigatória")
    @Size(max = 5)
    @Schema(description = "Série do RPS", example = "001")
    private String serie;

    @NotNull(message = "Tipo do RPS é obrigatório")
    @Min(1) @Max(3)
    @Schema(description = "Tipo do RPS: 1-RPS, 2-Nota Fiscal Conjugada (Mista), 3-Cupom", example = "1")
    private Integer tipo;

    @NotNull(message = "Data de emissão é obrigatória")
    @Schema(description = "Data e hora de emissão do RPS")
    private LocalDateTime dataEmissao;

    @NotNull(message = "Natureza da operação é obrigatória")
    @Min(1) @Max(6)
    @Schema(description = "Natureza da operação: 1-Tributação no município, 2-Tributação fora do município, 3-Isenção, 4-Imune, 5-Exigibilidade suspensa, 6-Sem incidência", example = "1")
    private Integer naturezaOperacao;

    @Min(1) @Max(6)
    @Schema(description = "Regime especial de tributação: 1-Microempresa Municipal, 2-Estimativa, 3-Sociedade de Profissionais, 4-Cooperativa, 5-MEI, 6-ME/EPP")
    private Integer regimeEspecialTributacao;

    @NotNull(message = "Optante pelo Simples Nacional é obrigatório")
    @Min(1) @Max(2)
    @Schema(description = "Optante pelo Simples Nacional: 1-Sim, 2-Não", example = "2")
    private Integer simplesNacional;

    @NotNull(message = "Incentivo fiscal é obrigatório")
    @Min(1) @Max(2)
    @Schema(description = "Incentivador cultural: 1-Sim, 2-Não", example = "2")
    private Integer incentivadorCultural;

    @NotNull(message = "Status do RPS é obrigatório")
    @Min(1) @Max(2)
    @Schema(description = "Status: 1-Normal, 2-Cancelado", example = "1")
    private Integer status;

    // Dados do Serviço
    @NotNull(message = "Valor dos serviços é obrigatório")
    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    @Schema(description = "Valor total dos serviços", example = "1500.00")
    private BigDecimal valorServicos;

    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    @Schema(description = "Valor das deduções", example = "0.00")
    private BigDecimal valorDeducoes;

    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    @Schema(description = "Valor do PIS", example = "0.00")
    private BigDecimal valorPis;

    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    @Schema(description = "Valor do COFINS", example = "0.00")
    private BigDecimal valorCofins;

    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    @Schema(description = "Valor do INSS", example = "0.00")
    private BigDecimal valorInss;

    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    @Schema(description = "Valor do IR", example = "0.00")
    private BigDecimal valorIr;

    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    @Schema(description = "Valor do CSLL", example = "0.00")
    private BigDecimal valorCsll;

    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    @Schema(description = "Valor de outras retenções", example = "0.00")
    private BigDecimal valorOutrasRetencoes;

    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    @Schema(description = "Valor ISS retido", example = "0.00")
    private BigDecimal valorIssRetido;

    @DecimalMin(value = "0.00")
    @Digits(integer = 5, fraction = 4)
    @Schema(description = "Alíquota do ISS", example = "0.0500")
    private BigDecimal aliquota;

    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    @Schema(description = "Valor do desconto incondicionado", example = "0.00")
    private BigDecimal descontoIncondicionado;

    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    @Schema(description = "Valor do desconto condicionado", example = "0.00")
    private BigDecimal descontoCondicionado;

    @NotBlank(message = "Item da lista de serviço é obrigatório")
    @Size(max = 5)
    @Schema(description = "Item da lista de serviço LC 116/2003", example = "01.07")
    private String itemListaServico;

    @Size(max = 7)
    @Schema(description = "Código CNAE", example = "6201501")
    private String codigoCnae;

    @Size(max = 20)
    @Schema(description = "Código de tributação do município")
    private String codigoTributacaoMunicipio;

    @NotBlank(message = "Discriminação do serviço é obrigatória")
    @Size(max = 2000)
    @Schema(description = "Descrição detalhada do serviço prestado")
    private String discriminacao;

    @NotNull(message = "Código do município é obrigatório")
    @Schema(description = "Código IBGE do município de prestação do serviço", example = "3548906")
    private Integer codigoMunicipio;
}
