package br.com.rentafit.auth.dto;

import br.com.rentafit.auth.domain.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to change a user's access role")
public record UpdateUserRoleRequestDTO(
        @NotNull(message = "Role is required")
        @Schema(description = "Target role", example = "EMPLOYEE")
        RoleName role,

        @Size(max = 10, message = "Initials must not exceed 10 characters")
        @Schema(description = "Employee initials required when elevating to EMPLOYEE/MANAGER and no Employee row exists yet",
                example = "JD")
        String initials,

        @Min(value = 1, message = "Role level must be at least 1")
        @Max(value = 10, message = "Role level must not exceed 10")
        @Schema(description = "Employee role level (defaults to 1 when null)", example = "1")
        Integer roleLevel
) {
}
