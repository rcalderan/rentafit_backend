package br.com.rentafit.people.controller;

import br.com.rentafit.people.dto.ActiveAttendantDTO;
import br.com.rentafit.people.dto.EmployeeAuthResponseDTO;
import br.com.rentafit.people.dto.EmployeeCheckRequestDTO;
import br.com.rentafit.people.dto.EmployeeCheckResponseDTO;
import br.com.rentafit.people.dto.EmployeeCreateRequestDTO;
import br.com.rentafit.people.dto.EmployeeDTO;
import br.com.rentafit.people.service.EmployeeService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee management APIs")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @Operation(summary = "List all employees with pagination")
    @ApiResponse(responseCode = "200", description = "Employees retrieved successfully")
    public ResponseEntity<Page<EmployeeDTO>> findAll(Pageable pageable) {
        return ResponseEntity.ok(employeeService.findAll(pageable));
    }

    @GetMapping("/attendants")
    @Operation(summary = "List active users eligible to attend rental items")
    @ApiResponse(responseCode = "200", description = "Active attendants retrieved successfully")
    public ResponseEntity<List<ActiveAttendantDTO>> findActiveAttendants() {
        return ResponseEntity.ok(employeeService.findActiveAttendants());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Employee found"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @GetMapping("/initials/{initials}")
    @Operation(summary = "Get employee by initials")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Employee found"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeCheckResponseDTO> findByInitials(@PathVariable String initials) {
        return ResponseEntity.ok(employeeService.findByInitials(initials));
    }

    @PostMapping
    @Operation(summary = "Create a new employee")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Employee created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<EmployeeCheckResponseDTO> create(@Valid @RequestBody EmployeeCreateRequestDTO dto) {
        EmployeeCheckResponseDTO created = employeeService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/check")
    @Operation(summary = "Check employee by initials and PIN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Employee credentials validated"),
        @ApiResponse(responseCode = "422", description = "Invalid employee credentials")
    })
    public ResponseEntity<EmployeeCheckResponseDTO> check(@Valid @RequestBody EmployeeCheckRequestDTO dto) {
        return ResponseEntity.ok(employeeService.check(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing employee")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.ok(employeeService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Employee deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
