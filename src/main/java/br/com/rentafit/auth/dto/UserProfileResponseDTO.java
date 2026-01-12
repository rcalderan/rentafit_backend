package br.com.rentafit.auth.dto;

import br.com.rentafit.auth.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile information")
public class UserProfileResponseDTO {
    
    @Schema(description = "User unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "Login username", example = "admin")
    private String username;
    
    @Schema(description = "User email address", example = "admin@rentafit.com")
    private String email;
    
    @Schema(description = "User full name", example = "Administrator")
    private String fullName;
    
    @Schema(description = "User access role", example = "ROLE_ADMIN")
    private UserRole role;
    
    @Schema(description = "Account active status", example = "true")
    private boolean active;
    
    @Schema(description = "Account creation date")
    private OffsetDateTime createdAt;
}
