package br.com.rentafit.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requisição para emissão de NF-e (modelo 55)")
public class NfeEmissionRequest {

    @NotNull(message = "ID do cliente é obrigatório")
    @Schema(description = "ID do cliente (destinatário)")
    private UUID customerId;

    @NotBlank(message = "Natureza da operação é obrigatória")
    @Schema(description = "Natureza da operação (natOp), ex.: Venda de mercadoria")
    private String natureOperation;

    @NotEmpty(message = "A NF-e deve conter ao menos um item")
    @Valid
    @Schema(description = "Itens (produtos) da nota")
    private List<NfeItemRequest> items;

    @Schema(description = "ID da entidade de origem (pedido de venda, etc.)")
    private UUID originId;

    @Schema(description = "Tipo de origem: SALES, RENTAL, MANUAL")
    private String origin;
}
