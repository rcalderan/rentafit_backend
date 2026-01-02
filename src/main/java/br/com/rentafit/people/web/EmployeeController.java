package br.com.rentafit.people.web;

import br.com.rentafit.people.dto.EmployeeDTO;
import br.com.rentafit.people.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee management APIs")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @Operation(summary = "List all employees with pagination")
    public Page<EmployeeDTO> findAll(Pageable pageable) {
        return employeeService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID")
    public EmployeeDTO findById(@PathVariable UUID id) {
        return employeeService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new employee")
    public EmployeeDTO create(@RequestBody EmployeeDTO dto) {
        return employeeService.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing employee")
    public EmployeeDTO update(@PathVariable UUID id, @RequestBody EmployeeDTO dto) {
        return employeeService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an employee")
    public void delete(@PathVariable UUID id) {
        employeeService.delete(id);
    }
}
