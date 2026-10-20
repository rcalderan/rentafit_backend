package br.com.rentafit.sector.controller;

import br.com.rentafit.sector.dto.CreateSectorRequestDTO;
import br.com.rentafit.sector.dto.SectorDTO;
import br.com.rentafit.sector.dto.UpdateSectorRequestDTO;
import br.com.rentafit.sector.service.SectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de gerenciamento de setores vinculados a Matrizes/Filiais (CNPJ).
 *
 * <p>Leitura liberada para EMPLOYEE/MANAGER/ADMIN (terminals precisam listar
 * setores para configuração); escrita restrita a MANAGER/ADMIN via SecurityConfig.</p>
 */
@RestController
@RequestMapping("/api/v1/sectors")
@RequiredArgsConstructor
@Slf4j
public class SectorController {

    private final SectorService sectorService;

    @GetMapping
    public ResponseEntity<List<SectorDTO>> list(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        return ResponseEntity.ok(sectorService.listAll(activeOnly));
    }

    @PostMapping
    public ResponseEntity<SectorDTO> create(@Valid @RequestBody CreateSectorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectorService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SectorDTO> update(@PathVariable UUID id,
                                            @Valid @RequestBody UpdateSectorRequestDTO dto) {
        return ResponseEntity.ok(sectorService.update(id, dto));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<SectorDTO> setDefault(@PathVariable UUID id) {
        return ResponseEntity.ok(sectorService.setDefault(id));
    }
}
