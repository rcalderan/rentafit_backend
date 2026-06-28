package br.com.rentafit.rental.dto.report;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Relatório diário de locação (checklist de preparação) para uma data de evento.
 *
 * <p>Os itens vêm agrupados por tipo de roupa ({@code clothingType}) para conferência
 * item a item no impresso. Totais são pré-calculados para o cabeçalho.</p>
 */
public record DailyRentalReportDTO(
        LocalDate eventDate,
        OffsetDateTime generatedAt,
        int contractCount,
        int itemCount,
        int adjustmentCount,
        List<ClothingTypeGroupDTO> groups
) {
}
