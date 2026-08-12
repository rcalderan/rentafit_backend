package br.com.rentafit.rental.dto.report;

/**
 * Ajuste/observação anotado em um item do contrato (metadata).
 * {@code type} é o nome do enum {@code ItemMetaType} (ACESSORIO|OBSERVACAO).
 */
public record ReportAdjustmentDTO(
        String type,
        String typeDescription,
        String description
) {
}
