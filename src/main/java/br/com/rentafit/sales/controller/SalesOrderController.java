package br.com.rentafit.sales.controller;

import br.com.rentafit.sales.domain.enums.SalesOrderStatus;
import br.com.rentafit.sales.dto.*;
import br.com.rentafit.sales.service.SalesOrderService;
import br.com.rentafit.sales.service.SalesWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales/orders")
@RequiredArgsConstructor
@Tag(name = "Sales Orders", description = "Gerenciamento de pedidos de venda (varejo)")
public class SalesOrderController {

    private final SalesOrderService orderService;
    private final SalesWorkflowService workflowService;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Listar pedidos (paginado, com filtros opcionais)")
    @ApiResponse(responseCode = "200", description = "Pedidos retornados com sucesso")
    public ResponseEntity<Page<SalesOrderSummaryDTO>> findAll(
            @RequestParam(required = false) SalesOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            Pageable pageable) {

        if (status != null || dateFrom != null || dateTo != null) {
            return ResponseEntity.ok(orderService.findWithFilters(status, dateFrom, dateTo, pageable));
        }
        return ResponseEntity.ok(orderService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pedido por UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<SalesOrderDetailsDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @GetMapping("/legacyId/{legacyId}")
    @Operation(summary = "Buscar pedido por código legado (V-YYYYMMDD-N)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public ResponseEntity<SalesOrderDetailsDTO> findByLegacyId(@PathVariable String legacyId) {
        return ResponseEntity.ok(orderService.findByLegacyId(legacyId));
    }

    @GetMapping("/byCustomer/{customerId}")
    @Operation(summary = "Listar pedidos de um cliente (histórico de compras)")
    public ResponseEntity<List<SalesOrderSummaryDTO>> findByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(orderService.findByCustomerId(customerId));
    }

    @PostMapping
    @Operation(summary = "Criar pedido de venda (DRAFT)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "422", description = "Validação de negócio falhou")
    })
    public ResponseEntity<SalesOrderDetailsDTO> create(@Valid @RequestBody CreateSalesOrderDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar pedido (somente DRAFT)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido atualizado"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "422", description = "Pedido não está em DRAFT")
    })
    public ResponseEntity<SalesOrderDetailsDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSalesOrderDTO dto) {
        return ResponseEntity.ok(orderService.update(id, dto));
    }

    // ── Workflow ──────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirmar pedido (DRAFT → CONFIRMED, reserva estoque)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido confirmado"),
            @ApiResponse(responseCode = "422", description = "Estoque insuficiente ou status inválido")
    })
    public ResponseEntity<SalesOrderDetailsDTO> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.confirm(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar pedido (DRAFT|CONFIRMED → CANCELLED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado"),
            @ApiResponse(responseCode = "422", description = "Status inválido para cancelamento")
    })
    public ResponseEntity<SalesOrderDetailsDTO> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelSalesOrderDTO dto) {
        return ResponseEntity.ok(workflowService.cancel(id, dto));
    }

    @PatchMapping("/{id}/items/{itemId}/ready")
    @Operation(summary = "Marcar item como pronto (RESERVED → READY)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item marcado como pronto"),
            @ApiResponse(responseCode = "422", description = "Status inválido")
    })
    public ResponseEntity<SalesOrderDetailsDTO> markItemReady(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(workflowService.markItemReady(id, itemId));
    }

    @PatchMapping("/{id}/items/{itemId}/deliver")
    @Operation(summary = "Confirmar entrega do item (READY → DELIVERED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item entregue"),
            @ApiResponse(responseCode = "422", description = "Item não está READY ou pedido não está PAID")
    })
    public ResponseEntity<SalesOrderDetailsDTO> deliverItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestParam(required = false) UUID employeeId) {
        return ResponseEntity.ok(workflowService.deliverItem(id, itemId, employeeId));
    }
}
