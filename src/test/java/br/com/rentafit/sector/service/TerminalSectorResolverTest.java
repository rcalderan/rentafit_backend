package br.com.rentafit.sector.service;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.sector.domain.Sector;
import br.com.rentafit.sector.repository.SectorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TerminalSectorResolver - Unit Tests")
class TerminalSectorResolverTest {

    @Mock private SectorRepository sectorRepository;

    @InjectMocks
    private TerminalSectorResolver resolver;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("resolveTerminalSector")
    class Resolve {

        @Test
        @DisplayName("Deve retornar vazio fora de contexto HTTP")
        void emptyWithoutRequestContext() {
            RequestContextHolder.resetRequestAttributes();

            assertThat(resolver.resolveTerminalSector()).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar vazio quando o header está ausente")
        void emptyWithoutHeader() {
            bindRequest(null);

            assertThat(resolver.resolveTerminalSector()).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar vazio quando o header não é UUID válido")
        void emptyWithInvalidUuid() {
            bindRequest("not-a-uuid");

            assertThat(resolver.resolveTerminalSector()).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar vazio quando o setor está inativo")
        void emptyWhenSectorInactive() {
            UUID id = UUID.randomUUID();
            Sector inactive = Sector.builder().id(id).active(false).build();
            when(sectorRepository.findById(id)).thenReturn(Optional.of(inactive));
            bindRequest(id.toString());

            assertThat(resolver.resolveTerminalSector()).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar o setor quando o header aponta para setor ativo")
        void returnsActiveSector() {
            UUID id = UUID.randomUUID();
            Sector sector = Sector.builder().id(id).active(true).name("Balcão").build();
            when(sectorRepository.findById(id)).thenReturn(Optional.of(sector));
            bindRequest(id.toString());

            assertThat(resolver.resolveTerminalSector()).contains(sector);
        }
    }

    @Nested
    @DisplayName("requireTerminalSector")
    class Require {

        @Test
        @DisplayName("Deve lançar ValidationException quando o terminal não informa setor")
        void throwsWithoutSector() {
            bindRequest(null);

            assertThatThrownBy(() -> resolver.requireTerminalSector())
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("setor");
        }

        @Test
        @DisplayName("Deve retornar o setor quando configurado")
        void returnsSector() {
            UUID id = UUID.randomUUID();
            Sector sector = Sector.builder().id(id).active(true).build();
            when(sectorRepository.findById(id)).thenReturn(Optional.of(sector));
            bindRequest(id.toString());

            assertThat(resolver.requireTerminalSector()).isEqualTo(sector);
        }
    }

    private void bindRequest(String headerValue) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (headerValue != null) {
            request.addHeader(TerminalSectorResolver.TERMINAL_SECTOR_HEADER, headerValue);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
