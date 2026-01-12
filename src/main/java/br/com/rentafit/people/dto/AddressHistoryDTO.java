package br.com.rentafit.people.dto;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for address history records
 */
@Builder
public record AddressHistoryDTO(
    UUID id,
    String zipCode,
    String street,
    String neighborhood,
    String city,
    String state,
    String number,
    String complement,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    OffsetDateTime archivedAt
) {}

