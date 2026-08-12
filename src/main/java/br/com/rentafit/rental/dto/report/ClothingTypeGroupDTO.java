package br.com.rentafit.rental.dto.report;

import java.util.List;

/**
 * Grupo de itens do relatório diário agrupados por tipo de roupa
 * (nome de exibição da categoria, ou "Sem categoria" quando não resolvido).
 */
public record ClothingTypeGroupDTO(
        String clothingType,
        int itemCount,
        List<ReportItemDTO> items
) {
}
