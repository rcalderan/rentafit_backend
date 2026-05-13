package br.com.rentafit.people.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * Resposta mínima para validação de funcionário no endpoint /employees/check.
 */
@Builder
public record EmployeeCheckResponseDTO(
        UUID id,
        String initials,
        String name
) {
}

