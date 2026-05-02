package br.com.rentafit.rental.controller;

import br.com.rentafit.rental.dto.CloseReturnRequestDTO;
import br.com.rentafit.rental.dto.MarkReturnRequestDTO;
import br.com.rentafit.rental.dto.ReturnSummaryDTO;
import br.com.rentafit.rental.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints de devolução granular de contratos de locação.
 *
 * <p>Fluxo esperado:
 * <ol>
 *   <li>GET return-summary — carregar estado atual de devolução</li>
 *   <li>POST return-mark (0..N vezes) — marcar itens/acessórios como devolvidos</li>
 *   <li>POST return-close — fechar contrato após devolução completa</li>
 * </ol>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/rental/contracts/{contractId}")
@RequiredArgsConstructor
@Tag(name = "Rental Return", description = "Sistema de devolução granular de contratos")
public class ReturnController {

    private final ReturnService returnService;

    @GetMapping("/return-summary")
    @Operation(
            summary = "Resumo de devolução do contrato",
            description = "Retorna estado atual dos itens, acessórios e pagamentos para a tela de devolução. Disponível apenas para contratos FINALIZED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado"),
            @ApiResponse(responseCode = "422", description = "Contrato não está em status FINALIZED")
    })
    public ResponseEntity<ReturnSummaryDTO> getReturnSummary(@PathVariable UUID contractId) {
        return ResponseEntity.ok(returnService.getReturnSummary(contractId));
    }

    @PostMapping("/return-mark")
    @Operation(
            summary = "Marcar itens/acessórios como devolvidos",
            description = "Registra a devolução de itens e/ou acessórios. Pode ser chamado múltiplas vezes (devolução parcial). Itens já marcados são ignorados silenciosamente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marcações aplicadas, resumo atualizado retornado"),
            @ApiResponse(responseCode = "400", description = "Request inválido"),
            @ApiResponse(responseCode = "404", description = "Contrato ou item não encontrado"),
            @ApiResponse(responseCode = "422", description = "Contrato não está em status FINALIZED")
    })
    public ResponseEntity<ReturnSummaryDTO> markItemsReturned(
            @PathVariable UUID contractId,
            @Valid @RequestBody MarkReturnRequestDTO dto) {
        return ResponseEntity.ok(returnService.markItemsReturned(contractId, dto));
    }

    @PostMapping("/return-close")
    @Operation(
            summary = "Fechar contrato após devolução completa",
            description = "Finaliza o processo de devolução, transicionando o contrato para CLOSED. Exige que todos os itens e acessórios estejam devolvidos e todas as parcelas pagas. Aciona workflow de liberação de estoque internamente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato fechado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Request inválido"),
            @ApiResponse(responseCode = "404", description = "Contrato ou funcionário não encontrado"),
            @ApiResponse(responseCode = "422", description = "Devolução incompleta ou parcelas pendentes")
    })
    public ResponseEntity<ReturnSummaryDTO> closeReturn(
            @PathVariable UUID contractId,
            @Valid @RequestBody CloseReturnRequestDTO dto) {
        return ResponseEntity.ok(returnService.closeReturn(contractId, dto));
    }
}
