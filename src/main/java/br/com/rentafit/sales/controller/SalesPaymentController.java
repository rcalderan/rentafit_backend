package br.com.rentafit.sales.controller;

import br.com.rentafit.sales.dto.SalesOrderDetailsDTO;
import br.com.rentafit.sales.dto.SalesPaymentDetailsDTO;
import br.com.rentafit.sales.dto.SalesPaymentInputDTO;
import br.com.rentafit.sales.service.SalesPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales/orders/{orderId}/payments")
@RequiredArgsConstructor
@Tag(name = "Sales Payments", description = "Gerenciamento de pagamentos de vendas")
public class SalesPaymentController {

    private final SalesPaymentService paymentService;

    @GetMapping
    @Operation(summary = "Listar parcelas do pedido")
    @ApiResponse(responseCode = "200", description = "Parcelas retornadas")
    public ResponseEntity<List<SalesPaymentDetailsDTO>> listPayments(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.listPayments(orderId));
    }

    @PostMapping
    @Operation(summary = "Adicionar parcela ao pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parcela adicionada"),
            @ApiResponse(responseCode = "422", description = "Status inválido ou limite de parcelas atingido")
    })
    public ResponseEntity<SalesOrderDetailsDTO> addPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody SalesPaymentInputDTO dto) {
        return ResponseEntity.ok(paymentService.addPayment(orderId, dto));
    }

    @PutMapping("/{paymentId}")
    @Operation(summary = "Atualizar parcela")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parcela atualizada"),
            @ApiResponse(responseCode = "422", description = "Parcela não está PENDING ou pedido já pago")
    })
    public ResponseEntity<SalesOrderDetailsDTO> updatePayment(
            @PathVariable UUID orderId,
            @PathVariable UUID paymentId,
            @Valid @RequestBody SalesPaymentInputDTO dto) {
        return ResponseEntity.ok(paymentService.updatePayment(orderId, paymentId, dto));
    }

    @DeleteMapping("/{paymentId}")
    @Operation(summary = "Cancelar parcela")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parcela cancelada"),
            @ApiResponse(responseCode = "422", description = "Parcela não está PENDING")
    })
    public ResponseEntity<SalesOrderDetailsDTO> cancelPayment(
            @PathVariable UUID orderId,
            @PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.cancelPayment(orderId, paymentId));
    }
}
