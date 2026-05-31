package br.com.rentafit.auth.dto;

import br.com.rentafit.auth.domain.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change a user's access role")
public record UpdateUserRoleRequestDTO(
        @NotNull(message = "Role is required")
        @Schema(description = "Target role", example = "EMPLOYEE")
        RoleName role
) {}
