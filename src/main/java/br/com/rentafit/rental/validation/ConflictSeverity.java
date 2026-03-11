package br.com.rentafit.rental.validation;

/**
 * Severidade de um conflito de reserva de item.
 */
public enum ConflictSeverity {
    /** Mesmo eventDate — impede a transição de estado. */
    BLOCKING,
    /** Dentro de 3 dias — emite alerta mas não bloqueia. */
    WARNING
}

