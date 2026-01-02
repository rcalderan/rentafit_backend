package br.com.rentafit.people.dto;

import lombok.Builder;
import java.util.UUID;

@Builder
public record EmployeeDTO(
    UUID id,
    String name,
    String document,
    String email,
    String initials,
    Integer roleLevel
) {}
