package br.com.rentafit.rental.controller;

import br.com.rentafit.rental.dto.CloseReturnRequestDTO;
import br.com.rentafit.rental.dto.MarkReturnRequestDTO;
import br.com.rentafit.rental.dto.ReturnSummaryDTO;
import br.com.rentafit.rental.service.ReturnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReturnController - Unit Tests")
class ReturnControllerTest {

    @Mock
    private ReturnService returnService;

    @InjectMocks
    private ReturnController controller;

    private UUID contractId;
    private ReturnSummaryDTO returnSummaryDTO;
    private MarkReturnRequestDTO markReturnRequestDTO;
    private CloseReturnRequestDTO closeReturnRequestDTO;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();

        returnSummaryDTO = ReturnSummaryDTO.builder()
                .contractId(contractId)
                .legacyId("CTR001")
                .customerName("Maria Silva")
                .returnDate(LocalDate.now().plusDays(2))
                .actualReturnDate(LocalDate.now())
                .pendingCount(0)
                .isFullyReturned(true)
                .delayDays(0)
                .suggestedFine(BigDecimal.ZERO)
                .items(List.of())
                .paymentsPreview(List.of())
                .build();

        markReturnRequestDTO = new MarkReturnRequestDTO(
                "João",
                List.of(),
                false,
                null
        );

        closeReturnRequestDTO = new CloseReturnRequestDTO(
                UUID.randomUUID(),
                false,
                null
        );
    }

    @Test
    @DisplayName("GET / contracts/{contractId}/return-summary deve retornar 200 com resumo")
    void testGetReturnSummary_returns200() {
        when(returnService.getReturnSummary(contractId)).thenReturn(returnSummaryDTO);

        ResponseEntity<ReturnSummaryDTO> response = controller.getReturnSummary(contractId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().contractId()).isEqualTo(contractId);
        verify(returnService, times(1)).getReturnSummary(contractId);
    }

    @Test
    @DisplayName("POST / contracts/{contractId}/return-mark deve retornar 200 com resumo atualizado")
    void testMarkItemsReturned_returns200() {
        when(returnService.markItemsReturned(contractId, markReturnRequestDTO)).thenReturn(returnSummaryDTO);

        ResponseEntity<ReturnSummaryDTO> response = controller.markItemsReturned(contractId, markReturnRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().contractId()).isEqualTo(contractId);
        verify(returnService, times(1)).markItemsReturned(contractId, markReturnRequestDTO);
    }

    @Test
    @DisplayName("POST / contracts/{contractId}/return-close deve retornar 200 com contrato fechado")
    void testCloseReturn_returns200() {
        when(returnService.closeReturn(contractId, closeReturnRequestDTO)).thenReturn(returnSummaryDTO);

        ResponseEntity<ReturnSummaryDTO> response = controller.closeReturn(contractId, closeReturnRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().contractId()).isEqualTo(contractId);
        verify(returnService, times(1)).closeReturn(contractId, closeReturnRequestDTO);
    }
}
