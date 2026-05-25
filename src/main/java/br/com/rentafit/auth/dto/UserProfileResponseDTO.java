package br.com.rentafit.auth.dto;

import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Schema(description = "User profile information")
public class UserProfileResponseDTO {
    
    @Schema(description = "User unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Legacy Id", example = "1221")
    private Integer legacyId;
    
    @Schema(description = "Login username", example = "admin")
    private String username;

    @Schema(description = "FullName", example = "JOHN DOE")
    private String name;
    
    @Schema(description = "USER PIN", example = "XSEA")
    private String pin;
    
    @Schema(description = "User access role", example = "ROLE_MANAGER")
    private List<String> roles;
    
    @Schema(description = "Account active status", example = "true")
    private boolean isActive;

    @Schema(description = "Whether the password has expired and must be changed", example = "false")
    private boolean passwordExpired;

    public UserProfileResponseDTO(UserAccount user){
        this(user, 90);
    }

    public UserProfileResponseDTO(UserAccount user, long passwordExpiryDays){
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getPerson() != null ? user.getPerson().getName() : null;
        this.pin = user.getPin();
        this.legacyId = user.getPerson() != null ? user.getPerson().getLegacyId() : null;
        this.roles = user.getRoles() != null ? user.getRoles().stream()
                .map(role -> role.getRole().name())
                .toList() : List.of();
        this.isActive = Boolean.TRUE.equals(user.getIsActive());
        this.passwordExpired = isPasswordExpired(user.getPasswordChangedAt(), passwordExpiryDays);
    }

    private static boolean isPasswordExpired(java.time.OffsetDateTime passwordChangedAt, long expiryDays) {
        if (passwordChangedAt == null) {
            return true;
        }
        return java.time.OffsetDateTime.now().isAfter(passwordChangedAt.plusDays(expiryDays));
    }

}
