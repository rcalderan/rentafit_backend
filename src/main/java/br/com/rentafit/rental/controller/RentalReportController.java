package br.com.rentafit.rental.controller;

import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.dto.report.DailyRentalReportDTO;
import br.com.rentafit.rental.service.DailyRentalReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rental/reports")
@RequiredArgsConstructor
@Tag(name = "Rental Reports", description = "Relatórios operacionais de locação")
public class RentalReportController {

    private final DailyRentalReportService reportService;

    @GetMapping("/daily")
    @Operation(summary = "Relatório diário de locação (checklist por tipo de roupa)",
            description = "Lista os itens de locação com eventDate na data informada, agrupados por tipo de roupa, "
                    + "com seus ajustes/observações. Status padrão: SIGNED e FINALIZED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório gerado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetro 'date' ausente ou inválido")
    })
    public ResponseEntity<DailyRentalReportDTO> daily(
            @Parameter(description = "Data do evento (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Status a incluir (default: SIGNED, FINALIZED)")
            @RequestParam(required = false) List<ContractStatus> statuses) {
        return ResponseEntity.ok(reportService.generate(date, statuses));
    }
}
