package br.com.rentafit.people.controller;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.dto.AddressHistoryDTO;
import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.dto.CustomerDetailsDTO;
import br.com.rentafit.people.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer management APIs")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "List all customers with pagination")
    @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    public ResponseEntity<Page<CustomerDetailsDTO>> findAll(@Valid Pageable pageable) {
        return ResponseEntity.ok(customerService.findAll(pageable));
    }

    @GetMapping("/byId/{id}")
    @Operation(summary = "Get customer by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Customer found"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<CustomerDetailsDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @GetMapping("/byDocument/{document}")
    @Operation(summary = "Get customer by Document")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerDetailsDTO> findByDocument(@PathVariable String document) {
        try {
            return ResponseEntity.ok(customerService.findByDocument(document));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException notFoundException){
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/byLegacyId/{legacyId}")
    @Operation(summary = "Get customer by LegacyId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<CustomerDetailsDTO> findByLegacyId(@PathVariable Integer legacyId) {
        try{
            return ResponseEntity.ok(customerService.findByLegacyId(legacyId));
        } catch (ResourceNotFoundException notFoundException){
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/{id}/address-history")
    @Operation(
        summary = "Get customer address history",
        description = "Retrieves all historical addresses for a customer ordered by date"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Address history retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<List<AddressHistoryDTO>> getAddressHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getAddressHistory(id));
    }

    @PostMapping
    @Operation(summary = "Create a new customer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "UnprocessableEntity: Customer with this document already exists"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<CustomerDetailsDTO> create(@Valid @RequestBody CustomerDTO dto) {
        try {
            CustomerDetailsDTO created = customerService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (HttpClientErrorException e) {
            System.out.println(e.getMessage());
            return ResponseEntity.unprocessableEntity().build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return  ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing customer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerDetailsDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerDTO dto) {
        return ResponseEntity.ok(customerService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Customer deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
