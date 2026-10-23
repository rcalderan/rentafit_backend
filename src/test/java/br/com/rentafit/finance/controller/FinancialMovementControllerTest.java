package br.com.rentafit.finance.controller;

import br.com.rentafit.finance.dto.MonthlyMovementResponseDTO;
import br.com.rentafit.finance.dto.YearlyMovementResponseDTO;
import br.com.rentafit.finance.service.FinancialMovementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinancialMovementController - Unit Tests")
class FinancialMovementControllerTest {

    @Mock
    private FinancialMovementService movementService;

    @InjectMocks
    private FinancialMovementController controller;

    @Test
    @DisplayName("Deve retornar movimentação mensal com HTTP 200")
    void shouldReturnMonthlyMovement() {
        MonthlyMovementResponseDTO dto = MonthlyMovementResponseDTO.builder()
                .year(2026)
                .month(10)
                .paidTotal(new BigDecimal("1200.00"))
                .fineTotal(new BigDecimal("50.00"))
                .refundedTotal(new BigDecimal("100.00"))
                .netRealizedTotal(new BigDecimal("1150.00"))
                .pendingTotal(new BigDecimal("300.00"))
                .rentalNetTotal(new BigDecimal("950.00"))
                .saleNetTotal(new BigDecimal("200.00"))
                .rentalPendingTotal(new BigDecimal("300.00"))
                .salePendingTotal(BigDecimal.ZERO)
                .byPaymentMethod(List.of())
                .days(List.of())
                .transactions(List.of())
                .build();

        when(movementService.getMonthlyMovement(2026, 10, "ALL", null, null)).thenReturn(dto);

        ResponseEntity<MonthlyMovementResponseDTO> response = controller.getMonthly(2026, 10, "ALL", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(movementService).getMonthlyMovement(2026, 10, "ALL", null, null);
    }

    @Test
    @DisplayName("Deve retornar movimentação anual com HTTP 200")
    void shouldReturnYearlyMovement() {
        YearlyMovementResponseDTO dto = YearlyMovementResponseDTO.builder()
                .year(2026)
                .availableYears(List.of(2026, 2025))
                .paidTotal(new BigDecimal("15000.00"))
                .fineTotal(BigDecimal.ZERO)
                .refundedTotal(BigDecimal.ZERO)
                .netRealizedTotal(new BigDecimal("15000.00"))
                .pendingTotal(new BigDecimal("2000.00"))
                .rentalNetTotal(new BigDecimal("12000.00"))
                .saleNetTotal(new BigDecimal("3000.00"))
                .rentalPendingTotal(new BigDecimal("2000.00"))
                .salePendingTotal(BigDecimal.ZERO)
                .byPaymentMethod(List.of())
                .months(List.of())
                .build();

        when(movementService.getYearlyMovement(2026, "RENTAL", null, null)).thenReturn(dto);

        ResponseEntity<YearlyMovementResponseDTO> response = controller.getYearly(2026, "RENTAL", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(movementService).getYearlyMovement(2026, "RENTAL", null, null);
    }
}
