package br.com.rentafit.finance.controller;

import br.com.rentafit.finance.dto.MonthlyMovementResponseDTO;
import br.com.rentafit.finance.dto.YearlyMovementResponseDTO;
import br.com.rentafit.finance.service.FinancialMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST para consulta da movimentação financeira (diária, mensal e anual)
 * de aluguéis e vendas.
 */
@RestController
@RequestMapping("/api/v1/finance/movements")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@Tag(name = "Financial Movements", description = "Movimentação financeira consolidada de locações e vendas")
public class FinancialMovementController {

    private final FinancialMovementService movementService;

    /**
     * Retorna o consolidado financeiro de um mês (totais, dias do calendário, formas de pagamento e transações).
     *
     * <p>Exemplo: {@code GET /api/v1/finance/movements/monthly?year=2026&month=10&source=ALL}</p>
     */
    @GetMapping("/monthly")
    @Operation(summary = "Consultar movimentação financeira mensal e diária",
            description = "Consolida parcelas PAID, PENDING, MULTA e REFUNDED de aluguéis e vendas no mês.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimentação mensal retornada com sucesso"),
            @ApiResponse(responseCode = "422", description = "Ano, mês ou filtro de origem inválido")
    })
    public ResponseEntity<MonthlyMovementResponseDTO> getMonthly(
            @Parameter(description = "Ano (ex.: 2026)", required = true) @RequestParam int year,
            @Parameter(description = "Mês (1 a 12)", required = true) @RequestParam int month,
            @Parameter(description = "Origem: ALL, RENTAL ou SALE")
            @RequestParam(defaultValue = "ALL") String source,
            @Parameter(description = "Setor de caixa (UUID); omitir para todos")
            @RequestParam(required = false) java.util.UUID sectorId,
            @Parameter(description = "CNPJ do estabelecimento (Matriz/Filial); omitir para todos")
            @RequestParam(required = false) String issuerCnpj) {
        return ResponseEntity.ok(movementService.getMonthlyMovement(year, month, source, sectorId, issuerCnpj));
    }

    /**
     * Retorna o consolidado financeiro anual (totais por mês de Jan a Dez e por forma de pagamento).
     *
     * <p>Exemplo: {@code GET /api/v1/finance/movements/yearly?year=2026&source=ALL}</p>
     */
    @GetMapping("/yearly")
    @Operation(summary = "Consultar movimentação financeira anual",
            description = "Consolida parcelas PAID, PENDING, MULTA e REFUNDED de aluguéis e vendas nos 12 meses do ano.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimentação anual retornada com sucesso"),
            @ApiResponse(responseCode = "422", description = "Ano ou filtro de origem inválido")
    })
    public ResponseEntity<YearlyMovementResponseDTO> getYearly(
            @Parameter(description = "Ano (ex.: 2026)", required = true) @RequestParam int year,
            @Parameter(description = "Origem: ALL, RENTAL ou SALE")
            @RequestParam(defaultValue = "ALL") String source,
            @Parameter(description = "Setor de caixa (UUID); omitir para todos")
            @RequestParam(required = false) java.util.UUID sectorId,
            @Parameter(description = "CNPJ do estabelecimento (Matriz/Filial); omitir para todos")
            @RequestParam(required = false) String issuerCnpj) {
        return ResponseEntity.ok(movementService.getYearlyMovement(year, source, sectorId, issuerCnpj));
    }
}
