package br.com.rentafit.rental.controller;

import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.dto.report.DailyRentalReportDTO;
import br.com.rentafit.rental.service.DailyRentalReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RentalReportController - Unit Tests")
class RentalReportControllerTest {

    @Mock private DailyRentalReportService reportService;

    @InjectMocks
    private RentalReportController controller;

    @Test
    @DisplayName("daily delega para o service e retorna 200 com o relatório")
    void dailyReturnsReport() {
        LocalDate date = LocalDate.of(2026, 6, 28);
        DailyRentalReportDTO dto = new DailyRentalReportDTO(
                date, OffsetDateTime.now(), 0, 0, 0, List.of());
        when(reportService.generate(date, null)).thenReturn(dto);

        ResponseEntity<DailyRentalReportDTO> response = controller.daily(date, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
        verify(reportService).generate(date, null);
    }

    @Test
    @DisplayName("daily repassa os status informados ao service")
    void dailyForwardsStatuses() {
        LocalDate date = LocalDate.of(2026, 6, 28);
        List<ContractStatus> statuses = List.of(ContractStatus.FINALIZED);
        DailyRentalReportDTO dto = new DailyRentalReportDTO(
                date, OffsetDateTime.now(), 0, 0, 0, List.of());
        when(reportService.generate(date, statuses)).thenReturn(dto);

        controller.daily(date, statuses);

        verify(reportService).generate(date, statuses);
    }
}
