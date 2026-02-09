package br.com.rentafit.product.controller;

import br.com.rentafit.product.dto.StockDTO;
import br.com.rentafit.product.dto.StockMovementDTO;
import br.com.rentafit.product.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Stock management APIs")
public class StockController {

    private final StockService stockService;

    @GetMapping("/{productId}")
    @Operation(summary = "Get stock by product ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Stock not found")
    })
    public ResponseEntity<StockDTO> getStock(@PathVariable UUID productId) {
        return ResponseEntity.ok(stockService.getStockByProduct(productId));
    }

    @GetMapping("/low")
    @Operation(summary = "Get products with low stock")
    @ApiResponse(responseCode = "200", description = "Low stock products retrieved")
    public ResponseEntity<List<StockDTO>> getLowStock() {
        return ResponseEntity.ok(stockService.getLowStockProducts());
    }

    @GetMapping("/{productId}/movements")
    @Operation(summary = "Get stock movements for a product")
    @ApiResponse(responseCode = "200", description = "Stock movements retrieved")
    public ResponseEntity<List<StockMovementDTO>> getMovements(@PathVariable UUID productId) {
        return ResponseEntity.ok(stockService.getStockMovements(productId));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock for a product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock reserved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Void> reserve(
        @RequestParam UUID productId,
        @RequestParam Integer quantity,
        @RequestParam UUID userId) {
        stockService.reserveStock(productId, quantity, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/release")
    @Operation(summary = "Release reserved stock")
    @ApiResponse(responseCode = "200", description = "Reservation released successfully")
    public ResponseEntity<Void> release(
        @RequestParam UUID productId,
        @RequestParam Integer quantity,
        @RequestParam UUID userId) {
        stockService.releaseReservation(productId, quantity, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/add")
    @Operation(summary = "Add stock")
    @ApiResponse(responseCode = "200", description = "Stock added successfully")
    public ResponseEntity<Void> add(
        @RequestParam UUID productId,
        @RequestParam Integer quantity,
        @RequestParam UUID userId,
        @RequestParam(required = false) String notes) {
        stockService.addStock(productId, quantity, notes, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/remove")
    @Operation(summary = "Remove stock")
    @ApiResponse(responseCode = "200", description = "Stock removed successfully")
    public ResponseEntity<Void> remove(
        @RequestParam UUID productId,
        @RequestParam Integer quantity,
        @RequestParam UUID userId,
        @RequestParam(required = false) String notes) {
        stockService.removeStock(productId, quantity, notes, userId);
        return ResponseEntity.ok().build();
    }
}
