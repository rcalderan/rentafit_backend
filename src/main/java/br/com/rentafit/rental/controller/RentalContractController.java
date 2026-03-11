package br.com.rentafit.rental.controller;

import br.com.rentafit.rental.dto.*;
import br.com.rentafit.rental.service.RentalContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rental/contracts")
@RequiredArgsConstructor
@Tag(name = "Rental Contracts", description = "Gerenciamento de contratos de locação")
public class RentalContractController {

    private final RentalContractService contractService;

    @GetMapping
    @Operation(summary = "Listar contratos (paginado)")
    @ApiResponse(responseCode = "200", description = "Contratos retornados com sucesso")
    public ResponseEntity<Page<RentalContractSummaryDTO>> findAll(Pageable pageable) {
        return ResponseEntity.ok(contractService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar contrato por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato encontrado"),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado")
    })
    public ResponseEntity<RentalContractDetailsDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    @GetMapping("/byCustomer/{customerId}")
    @Operation(summary = "Listar contratos de um cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contratos do cliente retornados"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public ResponseEntity<Page<RentalContractSummaryDTO>> findByCustomer(
            @PathVariable UUID customerId, Pageable pageable) {
        return ResponseEntity.ok(contractService.findByCustomer(customerId, pageable));
    }

    @PostMapping
    @Operation(summary = "Criar proposta (DRAFT)",
            description = "Cria um contrato em rascunho. Não reserva itens nem valida conflitos de data.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proposta criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada (cliente/item não encontrado, datas inválidas)")
    })
    public ResponseEntity<RentalContractDetailsDTO> create(@Valid @RequestBody CreateRentalContractDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar proposta (apenas DRAFT)",
            description = "Atualiza os dados do contrato. Bloqueado se status != DRAFT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado"),
            @ApiResponse(responseCode = "422", description = "Contrato não está em DRAFT")
    })
    public ResponseEntity<RentalContractDetailsDTO> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateRentalContractDTO dto) {
        return ResponseEntity.ok(contractService.update(id, dto));
    }

    @PatchMapping("/{id}/sign")
    @Operation(summary = "Assinar contrato (DRAFT → SIGNED)",
            description = "Executa checagem de conflitos de reserva. Conflito no mesmo dia bloqueia (422). "
                    + "Conflito dentro de 3 dias retorna alerta no campo 'warnings'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato assinado. Pode conter warnings."),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado"),
            @ApiResponse(responseCode = "422", description = "Status inválido ou conflito bloqueante")
    })
    public ResponseEntity<RentalContractDetailsDTO> sign(@PathVariable UUID id) {
        return ResponseEntity.ok(contractService.sign(id));
    }

    @PatchMapping("/{id}/finalize")
    @Operation(summary = "Finalizar contrato (SIGNED → FINALIZED)",
            description = "Reserva itens e acessórios. Re-valida conflitos. Após FINALIZED só é permitido adicionar pagamentos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato finalizado. Pode conter warnings."),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado"),
            @ApiResponse(responseCode = "422", description = "Status inválido, conflito bloqueante ou contrato sem itens")
    })
    public ResponseEntity<RentalContractDetailsDTO> finalize(@PathVariable UUID id) {
        return ResponseEntity.ok(contractService.finalize(id));
    }

    @PatchMapping("/{id}/return")
    @Operation(summary = "Processar devolução",
            description = "Registra devolução e envia itens para manutenção. Disponível somente para contratos FINALIZED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolução processada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado"),
            @ApiResponse(responseCode = "422", description = "Status inválido ou devolução já processada")
    })
    public ResponseEntity<RentalContractDetailsDTO> processReturn(
            @PathVariable UUID id, @Valid @RequestBody ReturnContractDTO dto) {
        return ResponseEntity.ok(contractService.processReturn(id, dto));
    }

    @PatchMapping("/{id}/items/{itemId}/deliver")
    @Operation(summary = "Confirmar entrega de um item",
            description = "Marca um item como entregue (retirado pelo cliente) e atualiza status para RENTED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega confirmada"),
            @ApiResponse(responseCode = "404", description = "Contrato ou item não encontrado"),
            @ApiResponse(responseCode = "422", description = "Contrato não está FINALIZED ou item já entregue")
    })
    public ResponseEntity<RentalContractDetailsDTO> deliverItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Parameter(description = "UUID do funcionário que registrou a entrega")
            @RequestParam(required = false) UUID attendantEmployeeId) {
        return ResponseEntity.ok(contractService.deliverItem(id, itemId, attendantEmployeeId));
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Duplicar contrato como novo DRAFT",
            description = "Clona o contrato com snapshot atualizado do cliente, mesmos itens e sem pagamentos.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contrato duplicado como novo DRAFT"),
            @ApiResponse(responseCode = "404", description = "Contrato original não encontrado")
    })
    public ResponseEntity<RentalContractDetailsDTO> duplicate(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.duplicate(id));
    }
}

