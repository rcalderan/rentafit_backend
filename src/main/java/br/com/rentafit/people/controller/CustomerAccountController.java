package br.com.rentafit.people.controller;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.people.dto.CustomerAccountHistoryDTO;
import br.com.rentafit.rental.dto.RentalContractSummaryDTO;
import br.com.rentafit.rental.service.RentalContractService;
import br.com.rentafit.sales.service.SalesOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service area for authenticated customers.
 * All data is scoped to the authenticated user — no customerId path param to prevent IDOR.
 */
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Tag(name = "Minha Conta", description = "Área do cliente autenticado — locações e compras próprias")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class CustomerAccountController {

    private final RentalContractService rentalContractService;
    private final SalesOrderService salesOrderService;

    @GetMapping("/rentals")
    @Operation(summary = "Minhas locações",
               description = "Retorna as locações do cliente autenticado, paginadas por data de criação desc.")
    @ApiResponse(responseCode = "200", description = "Locações retornadas com sucesso")
    public ResponseEntity<Page<RentalContractSummaryDTO>> myRentals(
            @AuthenticationPrincipal UserAccount principal,
            Pageable pageable) {
        return ResponseEntity.ok(rentalContractService.findByCustomer(principal.getId(), pageable));
    }

    @GetMapping("/history")
    @Operation(summary = "Meu histórico completo",
               description = "Retorna locações e compras do cliente autenticado em uma única chamada.")
    @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso")
    public ResponseEntity<CustomerAccountHistoryDTO> myHistory(
            @AuthenticationPrincipal UserAccount principal,
            Pageable pageable) {
        var rentals = rentalContractService.findByCustomer(principal.getId(), pageable);
        var orders  = salesOrderService.findByCustomerId(principal.getId());
        return ResponseEntity.ok(new CustomerAccountHistoryDTO(rentals, orders));
    }
}
