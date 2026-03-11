package br.com.rentafit.rental.controller;

import br.com.rentafit.rental.dto.RentalPaymentDetailsDTO;
import br.com.rentafit.rental.dto.RentalPaymentInputDTO;
import br.com.rentafit.rental.service.RentalPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rental/contracts/{contractId}/payments")
@RequiredArgsConstructor
@Tag(name = "Rental Payments", description = "Gerenciamento de parcelas de pagamento")
public class RentalPaymentController {

    private final RentalPaymentService paymentService;

    @GetMapping
    @Operation(summary = "Listar pagamentos do contrato")
    @ApiResponse(responseCode = "200", description = "Pagamentos retornados com sucesso")
    public ResponseEntity<List<RentalPaymentDetailsDTO>> listByContract(@PathVariable UUID contractId) {
        return ResponseEntity.ok(paymentService.listByContract(contractId));
    }

    @PostMapping
    @Operation(summary = "Adicionar parcela",
            description = "Adiciona uma parcela ao contrato. Permitido mesmo após FINALIZED.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Parcela adicionada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "422", description = "Regra de pagamento violada")
    })
    public ResponseEntity<RentalPaymentDetailsDTO> addPayment(
            @PathVariable UUID contractId,
            @Valid @RequestBody RentalPaymentInputDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.addPayment(contractId, dto));
    }

    @PutMapping("/{paymentId}")
    @Operation(summary = "Atualizar parcela",
            description = "Atualiza uma parcela. Bloqueado após FINALIZED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parcela atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Parcela não encontrada"),
            @ApiResponse(responseCode = "422", description = "Contrato FINALIZED ou regra violada")
    })
    public ResponseEntity<RentalPaymentDetailsDTO> updatePayment(
            @PathVariable UUID contractId,
            @PathVariable UUID paymentId,
            @Valid @RequestBody RentalPaymentInputDTO dto) {
        return ResponseEntity.ok(paymentService.updatePayment(contractId, paymentId, dto));
    }

    @DeleteMapping("/{paymentId}")
    @Operation(summary = "Cancelar parcela",
            description = "Cancela uma parcela. Bloqueado após FINALIZED.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Parcela cancelada"),
            @ApiResponse(responseCode = "404", description = "Parcela não encontrada"),
            @ApiResponse(responseCode = "422", description = "Contrato FINALIZED")
    })
    public ResponseEntity<Void> cancelPayment(
            @PathVariable UUID contractId,
            @PathVariable UUID paymentId) {
        paymentService.cancelPayment(contractId, paymentId);
        return ResponseEntity.noContent().build();
    }
}

