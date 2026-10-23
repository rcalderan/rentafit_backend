package br.com.rentafit.finance.dto;

import br.com.rentafit.common.exception.ValidationException;

import java.util.Locale;

/**
 * Filtro de origem dos lançamentos financeiros.
 */
public enum MovementSourceFilter {
    ALL,
    RENTAL,
    SALE;

    /**
     * Converte o parâmetro textual da requisição para {@link MovementSourceFilter}.
     *
     * <p>Exemplo: {@code MovementSourceFilter.fromParam("RENTAL")}</p>
     */
    public static MovementSourceFilter fromParam(String rawSource) {
        if (rawSource == null || rawSource.isBlank()) {
            return ALL;
        }
        String normalized = rawSource.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALL" -> ALL;
            case "RENTAL" -> RENTAL;
            case "SALE" -> SALE;
            default -> throw new ValidationException(
                    "Invalid movement source '" + rawSource + "'. Expected one of: ALL, RENTAL, SALE");
        };
    }

    public boolean includesRental() {
        return this == ALL || this == RENTAL;
    }

    public boolean includesSale() {
        return this == ALL || this == SALE;
    }
}
