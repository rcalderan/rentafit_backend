package br.com.rentafit.rental.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reserva ativa de um item de locação: contrato SIGNED/FINALIZED com eventDate
 * a partir de hoje que contém o item.
 *
 * <p>Retornado por {@code GET /api/v1/rental/contracts/byItem/{rentalItemId}}.
 * O frontend usa {@code contractId} para abrir a aba do contrato
 * ({@code /rental/new?id=...}), no mesmo padrão de "Últimos Contratos".</p>
 */
public record ItemReservationDTO(
        UUID contractId,
        String legacyId,
        UUID customerId,
        String customerName,
        Integer customerLegacyId,
        LocalDate eventDate,
        LocalDate pickupDate,
        LocalDate returnDate,
        String status,
        String statusDescription
) {}
