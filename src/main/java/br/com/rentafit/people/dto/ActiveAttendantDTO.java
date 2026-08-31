package br.com.rentafit.people.dto;

import br.com.rentafit.auth.domain.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Active user eligible to attend a rental item")
public record ActiveAttendantDTO(
        UUID id,
        String name,
        RoleName role
) {
}
