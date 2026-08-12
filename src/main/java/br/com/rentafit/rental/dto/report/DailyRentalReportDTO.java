package br.com.rentafit.rental.dto.report;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Relatório de locação por período (checklist de preparação).
 *
 * <p>Cobre contratos cujo {@code eventDate} esteja entre {@code startDate} e {@code endDate}
 * (ambos inclusivos). Para relatório de um único dia, {@code startDate == endDate}.</p>
 *
 * <p>Os itens vêm agrupados por tipo de roupa ({@code clothingType}) para conferência
 * item a item no impresso. Totais são pré-calculados para o cabeçalho.</p>
 */
public record DailyRentalReportDTO(
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime generatedAt,
        int contractCount,
        int itemCount,
        int adjustmentCount,
        List<ClothingTypeGroupDTO> groups
) {
}
