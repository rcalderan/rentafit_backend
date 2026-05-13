package br.com.rentafit.people.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record EmployeeAuthResponseDTO(
        UUID id,
        String name,
        String document,
        String email,
        String initials,
        Integer roleLevel
) {
}

