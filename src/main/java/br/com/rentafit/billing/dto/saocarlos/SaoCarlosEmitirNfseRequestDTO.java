package br.com.rentafit.billing.dto.saocarlos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
@Schema(description = "Request para emissão de NFS-e São Carlos")
public class SaoCarlosEmitirNfseRequestDTO {

    @NotNull(message = "ID do cliente é obrigatório")
    @Schema(description = "UUID do cliente (tomador do serviço)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID customerId;

    @NotEmpty(message = "Deve haver pelo menos um RPS")
    @Valid
    @Schema(description = "Lista de RPS a serem enviados no lote")
    private List<SaoCarlosRpsDTO> rps;

    @Valid
    @Schema(description = "Dados do tomador do serviço")
    private SaoCarlosTomadorDTO tomador;
}
