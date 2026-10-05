package br.com.rentafit.rental.validation;

import java.util.UUID;

/**
 * Referência mínima de um item para checagem de conflito de reserva.
 * Permite aplicar a mesma regra a entidades persistidas (transições)
 * e a DTOs de entrada (salvamento de proposta, antes de existir entidade).
 */
public record RentalItemRef(UUID rentalItemId, String description) {}
