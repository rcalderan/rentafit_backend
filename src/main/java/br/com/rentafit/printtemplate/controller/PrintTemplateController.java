package br.com.rentafit.printtemplate.controller;

import br.com.rentafit.printtemplate.dto.PrintTemplateRequest;
import br.com.rentafit.printtemplate.dto.PrintTemplateResponse;
import br.com.rentafit.printtemplate.service.PrintTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/print-templates")
@RequiredArgsConstructor
@Validated
@Tag(name = "Print Templates", description = "Printable document templates")
public class PrintTemplateController {

    private final PrintTemplateService service;

    @GetMapping
    @Operation(summary = "List all print templates")
    public ResponseEntity<List<PrintTemplateResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a print template by ID")
    public ResponseEntity<PrintTemplateResponse> findById(@PathVariable @NotBlank @Size(max = 100) String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Create or replace a print template")
    public ResponseEntity<PrintTemplateResponse> save(
            @PathVariable @NotBlank @Size(max = 100) String id,
            @Valid @RequestBody PrintTemplateRequest request
    ) {
        boolean created = !service.existsById(id);
        PrintTemplateResponse response = service.save(id, request);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a print template")
    public ResponseEntity<Void> delete(@PathVariable @NotBlank @Size(max = 100) String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
