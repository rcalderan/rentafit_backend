package br.com.rentafit.auth.dto;

import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder
@Schema(description = "Summary of a user account for administration screens")
public record UserSummaryDTO(
        UUID id,
        String username,
        String name,
        RoleName role,
        boolean active
) {
    public static UserSummaryDTO fromEntity(UserAccount user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getPerson() != null ? user.getPerson().getName() : null)
                .role(user.getRole())
                .active(Boolean.TRUE.equals(user.getIsActive()))
                .build();
    }
}
