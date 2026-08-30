package br.com.rentafit.product.controller;

import br.com.rentafit.product.dto.rental.RentalItemDTO;
import br.com.rentafit.product.dto.rental.RentalItemDetailsDTO;
import br.com.rentafit.product.dto.rental.RentalItemUpdateDTO;
import br.com.rentafit.product.service.RentalItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/rental")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
public class RentalItemController {

    private final RentalItemService rentalItemService;

    @PostMapping
    @Operation(summary = "Create a new rental product")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "422", description = "Unprocessable Entity")
    })
    public ResponseEntity<RentalItemDetailsDTO> create(@Valid @RequestBody RentalItemDTO dto) {
        RentalItemDetailsDTO created = rentalItemService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<RentalItemDetailsDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(rentalItemService.findById(id));
    }

    @GetMapping
    @Operation(summary = "List all products with pagination")
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    public ResponseEntity<Page<RentalItemDetailsDTO>> findAll(@Valid Pageable pageable) {
        return ResponseEntity.ok(rentalItemService.findAll(pageable));
    }


    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "422", description = "Unprocessable Entity")
    })
    public ResponseEntity<RentalItemDetailsDTO> update(@PathVariable UUID id, @Valid @RequestBody RentalItemUpdateDTO dto) {
        return ResponseEntity.ok(rentalItemService.update(id, dto));
    }


    @GetMapping("/byLegacy/{id}")
    @Operation(summary = "Get product by legacyId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity")
    })
    public ResponseEntity<RentalItemDetailsDTO> findByLegacyId(@PathVariable Integer id) {
        return ResponseEntity.ok(rentalItemService.findByLegacyId(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "422", description = "Unprocessable Entity")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        rentalItemService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
