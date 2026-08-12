package br.com.rentafit.product.controller;

import br.com.rentafit.product.dto.retail.ProductRetailDTO;
import br.com.rentafit.product.dto.retail.ProductRetailDetailsDTO;
import br.com.rentafit.product.dto.retail.ProductRetailUpdateDTO;
import br.com.rentafit.product.service.RetailProductService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/products/retail")
@RequiredArgsConstructor
@Tag(name = "Retail Products", description = "Product Retail management APIs")
public class RetailProductController {

    private final RetailProductService retailProductService;

    @PostMapping
    @Operation(summary = "Create a new retail product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity")
    })
    public ResponseEntity<ProductRetailDetailsDTO> create(@Valid @RequestBody ProductRetailDTO dto) {
        ProductRetailDetailsDTO created = retailProductService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity")
    })
    public ResponseEntity<ProductRetailDetailsDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(retailProductService.findById(id));
    }

    @GetMapping
    @Operation(summary = "List all products with pagination")
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    public ResponseEntity<Page<ProductRetailDetailsDTO>> findAll(@Valid Pageable pageable) {
        return ResponseEntity.ok(retailProductService.findAll(pageable));
    }
    
    // @GetMapping("by-name/{name}")
    // @Operation(summary = "List all products by name")
    // @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    // public ResponseEntity<Page<ProductRetailDetailsDTO>> findAllByName(@PathVariable String name, @Valid Pageable pageable) {
    //     return ResponseEntity.ok(retailProductService.findAllByName(name, pageable));
    // }


    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity")
    })
    public ResponseEntity<ProductRetailDetailsDTO> update(@PathVariable UUID id, @Valid @RequestBody ProductRetailUpdateDTO dto) {
        return ResponseEntity.ok(retailProductService.update(id, dto));
    }


    @GetMapping("/bysku/{sku}")
    @Operation(summary = "Get product by sku")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductRetailDetailsDTO> findByLegacyId(@PathVariable String sku) {
        return ResponseEntity.ok(retailProductService.findBySku(sku));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        retailProductService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
