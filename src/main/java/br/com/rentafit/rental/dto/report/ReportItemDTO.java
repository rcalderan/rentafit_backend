package br.com.rentafit.rental.dto.report;

import java.time.LocalDate;
import java.util.List;

/**
 * Linha de item no relatório diário de locação.
 * Exibe dados de identificação, data de retirada e os ajustes/observações anotados.
 */
public record ReportItemDTO(
        String contractLegacyId,
        String customerName,
        String legacyProductCode,
        String description,
        String size,
        String color,
        LocalDate pickupDate,
        List<ReportAdjustmentDTO> adjustments
) {
}
