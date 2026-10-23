package br.com.rentafit.sector.controller;

import br.com.rentafit.sector.dto.CreateSectorRequestDTO;
import br.com.rentafit.sector.dto.SectorDTO;
import br.com.rentafit.sector.dto.UpdateSectorRequestDTO;
import br.com.rentafit.sector.service.SectorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SectorController - Unit Tests")
class SectorControllerTest {

    @Mock private SectorService sectorService;

    @InjectMocks
    private SectorController controller;

    @Test
    @DisplayName("GET / deve retornar 200 com a lista de setores")
    void listReturnsOk() {
        SectorDTO dto = SectorDTO.builder().id(UUID.randomUUID()).name("Balcão").build();
        when(sectorService.listAll(false)).thenReturn(List.of(dto));

        ResponseEntity<List<SectorDTO>> response = controller.list(false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(sectorService).listAll(false);
    }

    @Test
    @DisplayName("POST / deve retornar 201 com o setor criado")
    void createReturnsCreated() {
        CreateSectorRequestDTO request = new CreateSectorRequestDTO(
                "12345678000199", null, "Balcão", "BALCAO", false);
        SectorDTO dto = SectorDTO.builder().id(UUID.randomUUID()).name("Balcão").build();
        when(sectorService.create(request)).thenReturn(dto);

        ResponseEntity<SectorDTO> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("PUT /{id} deve delegar a atualização ao service")
    void updateDelegates() {
        UUID id = UUID.randomUUID();
        UpdateSectorRequestDTO request = new UpdateSectorRequestDTO(null, "Novo", null, null);
        SectorDTO dto = SectorDTO.builder().id(id).name("Novo").build();
        when(sectorService.update(id, request)).thenReturn(dto);

        ResponseEntity<SectorDTO> response = controller.update(id, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(sectorService).update(id, request);
    }

    @Test
    @DisplayName("PATCH /{id}/default deve delegar ao service")
    void setDefaultDelegates() {
        UUID id = UUID.randomUUID();
        SectorDTO dto = SectorDTO.builder().id(id).isDefault(true).build();
        when(sectorService.setDefault(id)).thenReturn(dto);

        ResponseEntity<SectorDTO> response = controller.setDefault(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isDefault()).isTrue();
        verify(sectorService).setDefault(id);
    }
}
