package br.com.rentafit.people.web;

import br.com.rentafit.people.dto.CustomerDTO;
import br.com.rentafit.people.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer management APIs")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "List all customers with pagination")
    public Page<CustomerDTO> findAll(Pageable pageable) {
        return customerService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public CustomerDTO findById(@PathVariable UUID id) {
        return customerService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new customer")
    public CustomerDTO create(@RequestBody CustomerDTO dto) {
        return customerService.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing customer")
    public CustomerDTO update(@PathVariable UUID id, @RequestBody CustomerDTO dto) {
        return customerService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a customer")
    public void delete(@PathVariable UUID id) {
        customerService.delete(id);
    }
}
