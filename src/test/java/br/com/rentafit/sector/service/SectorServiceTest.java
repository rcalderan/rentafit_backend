package br.com.rentafit.sector.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.sector.domain.Sector;
import br.com.rentafit.sector.dto.CreateSectorRequestDTO;
import br.com.rentafit.sector.dto.SectorDTO;
import br.com.rentafit.sector.dto.UpdateSectorRequestDTO;
import br.com.rentafit.sector.repository.SectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SectorService - Unit Tests")
class SectorServiceTest {

    @Mock private SectorRepository sectorRepository;

    @InjectMocks
    private SectorService sectorService;

    private UUID sectorId;
    private Sector sector;

    @BeforeEach
    void setUp() {
        sectorId = UUID.randomUUID();
        sector = Sector.builder()
                .id(sectorId)
                .issuerCnpj("12345678000199")
                .branchName("Matriz Centro")
                .name("Balcão")
                .code("BALCAO")
                .active(true)
                .isDefault(false)
                .build();
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("Deve listar apenas ativos quando activeOnly=true")
        void listsActiveOnly() {
            when(sectorRepository.findByActiveTrueOrderByIssuerCnpjAscNameAsc())
                    .thenReturn(List.of(sector));

            List<SectorDTO> result = sectorService.listAll(true);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("Balcão");
            verify(sectorRepository, never()).findAllByOrderByIssuerCnpjAscNameAsc();
        }

        @Test
        @DisplayName("Deve listar todos quando activeOnly=false")
        void listsAll() {
            when(sectorRepository.findAllByOrderByIssuerCnpjAscNameAsc())
                    .thenReturn(List.of(sector));

            List<SectorDTO> result = sectorService.listAll(false);

            assertThat(result).hasSize(1);
            verify(sectorRepository, never()).findByActiveTrueOrderByIssuerCnpjAscNameAsc();
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Deve criar setor com sigla normalizada em maiúsculas")
        void createsWithNormalizedCode() {
            CreateSectorRequestDTO dto = new CreateSectorRequestDTO(
                    "12345678000199", "Matriz Centro", "Depósito", "deposito", false);
            when(sectorRepository.existsByIssuerCnpjAndNameIgnoreCase(any(), any())).thenReturn(false);
            when(sectorRepository.existsByIssuerCnpjAndCodeIgnoreCase(any(), any())).thenReturn(false);
            when(sectorRepository.save(any(Sector.class))).thenAnswer(i -> i.getArgument(0));

            SectorDTO result = sectorService.create(dto);

            assertThat(result.code()).isEqualTo("DEPOSITO");
            assertThat(result.active()).isTrue();
        }

        @Test
        @DisplayName("Deve rejeitar nome duplicado no mesmo estabelecimento")
        void rejectsDuplicateName() {
            CreateSectorRequestDTO dto = new CreateSectorRequestDTO(
                    "12345678000199", null, "Balcão", "BALCAO", false);
            when(sectorRepository.existsByIssuerCnpjAndNameIgnoreCase("12345678000199", "Balcão"))
                    .thenReturn(true);

            assertThatThrownBy(() -> sectorService.create(dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Balcão");
            verify(sectorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve rejeitar sigla duplicada no mesmo estabelecimento")
        void rejectsDuplicateCode() {
            CreateSectorRequestDTO dto = new CreateSectorRequestDTO(
                    "12345678000199", null, "Novo Balcão", "balcao", false);
            when(sectorRepository.existsByIssuerCnpjAndNameIgnoreCase(any(), any())).thenReturn(false);
            when(sectorRepository.existsByIssuerCnpjAndCodeIgnoreCase("12345678000199", "BALCAO"))
                    .thenReturn(true);

            assertThatThrownBy(() -> sectorService.create(dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("BALCAO");
            verify(sectorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve remover flag de padrão do setor anterior ao criar novo padrão")
        void clearsPreviousDefault() {
            Sector previousDefault = Sector.builder()
                    .id(UUID.randomUUID()).issuerCnpj("12345678000199")
                    .name("Caixa").code("CAIXA").active(true).isDefault(true).build();
            CreateSectorRequestDTO dto = new CreateSectorRequestDTO(
                    "12345678000199", null, "Balcão", "BALCAO", true);
            when(sectorRepository.existsByIssuerCnpjAndNameIgnoreCase(any(), any())).thenReturn(false);
            when(sectorRepository.existsByIssuerCnpjAndCodeIgnoreCase(any(), any())).thenReturn(false);
            when(sectorRepository.findFirstByIssuerCnpjAndIsDefaultTrue("12345678000199"))
                    .thenReturn(Optional.of(previousDefault));
            when(sectorRepository.save(any(Sector.class))).thenAnswer(i -> i.getArgument(0));

            sectorService.create(dto);

            assertThat(previousDefault.getIsDefault()).isFalse();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Deve atualizar nome e sigla preservando campos nulos")
        void updatesFields() {
            when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));
            when(sectorRepository.existsByIssuerCnpjAndNameIgnoreCase(any(), any())).thenReturn(false);
            when(sectorRepository.existsByIssuerCnpjAndCodeIgnoreCase(any(), any())).thenReturn(false);
            when(sectorRepository.save(any(Sector.class))).thenAnswer(i -> i.getArgument(0));

            SectorDTO result = sectorService.update(sectorId,
                    new UpdateSectorRequestDTO(null, "Balcão Lateral", "balcao2", null));

            assertThat(result.name()).isEqualTo("Balcão Lateral");
            assertThat(result.code()).isEqualTo("BALCAO2");
            assertThat(result.active()).isTrue();
        }

        @Test
        @DisplayName("Deve rejeitar nome que colide com outro setor")
        void rejectsNameCollision() {
            when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));
            when(sectorRepository.existsByIssuerCnpjAndNameIgnoreCase("12345678000199", "Depósito"))
                    .thenReturn(true);

            assertThatThrownBy(() -> sectorService.update(sectorId,
                    new UpdateSectorRequestDTO(null, "Depósito", null, null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Depósito");
        }

        @Test
        @DisplayName("Deve lançar 404 quando o setor não existe")
        void rejectsUnknownId() {
            when(sectorRepository.findById(sectorId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sectorService.update(sectorId,
                    new UpdateSectorRequestDTO(null, "X", null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("setDefault")
    class SetDefault {

        @Test
        @DisplayName("Deve marcar como padrão e desmarcar o anterior")
        void marksDefaultAndClearsPrevious() {
            Sector previousDefault = Sector.builder()
                    .id(UUID.randomUUID()).issuerCnpj("12345678000199")
                    .name("Caixa").code("CAIXA").active(true).isDefault(true).build();
            when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));
            when(sectorRepository.findFirstByIssuerCnpjAndIsDefaultTrue("12345678000199"))
                    .thenReturn(Optional.of(previousDefault));
            when(sectorRepository.save(any(Sector.class))).thenAnswer(i -> i.getArgument(0));

            SectorDTO result = sectorService.setDefault(sectorId);

            assertThat(result.isDefault()).isTrue();
            assertThat(previousDefault.getIsDefault()).isFalse();
        }
    }
}
