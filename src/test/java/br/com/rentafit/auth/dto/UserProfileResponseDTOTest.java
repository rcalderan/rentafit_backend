package br.com.rentafit.auth.dto;

import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.domain.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserProfileResponseDTO - passwordExpired calculation")
class UserProfileResponseDTOTest {

    private UserAccount userAccount;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setRole(RoleName.ADMIN);

        userAccount = new UserAccount();
        userAccount.setId(UUID.randomUUID());
        userAccount.setUsername("admin");
        userAccount.setPassword("hash");
        userAccount.setRoles(List.of(adminRole));
        userAccount.setIsActive(true);
    }

    @Test
    @DisplayName("passwordExpired=true when passwordChangedAt is null (first access)")
    void testPasswordExpired_NullTimestamp() {
        userAccount.setPasswordChangedAt(null);
        var dto = new UserProfileResponseDTO(userAccount, 90);
        assertThat(dto.isPasswordExpired()).isTrue();
    }

    @Test
    @DisplayName("passwordExpired=true when password changed > expiryDays ago")
    void testPasswordExpired_Expired() {
        userAccount.setPasswordChangedAt(OffsetDateTime.now().minusDays(91));
        var dto = new UserProfileResponseDTO(userAccount, 90);
        assertThat(dto.isPasswordExpired()).isTrue();
    }

    @Test
    @DisplayName("passwordExpired=false when password changed recently")
    void testPasswordExpired_NotExpired() {
        userAccount.setPasswordChangedAt(OffsetDateTime.now().minusDays(10));
        var dto = new UserProfileResponseDTO(userAccount, 90);
        assertThat(dto.isPasswordExpired()).isFalse();
    }

    @Test
    @DisplayName("passwordExpired=false when changed exactly at boundary")
    void testPasswordExpired_AtBoundary() {
        userAccount.setPasswordChangedAt(OffsetDateTime.now().minusDays(89));
        var dto = new UserProfileResponseDTO(userAccount, 90);
        assertThat(dto.isPasswordExpired()).isFalse();
    }

    @Test
    @DisplayName("pinConfigured is true when UserAccount has a PIN hash")
    void testPinConfiguredTrue() {
        userAccount.setPin("$2a$10$someBcryptHashValue");
        userAccount.setPasswordChangedAt(OffsetDateTime.now());
        var dto = new UserProfileResponseDTO(userAccount, 90);
        assertThat(dto.isPinConfigured()).isTrue();
    }

    @Test
    @DisplayName("pinConfigured is false when PIN not set")
    void testPinConfiguredFalse() {
        userAccount.setPin(null);
        userAccount.setPasswordChangedAt(OffsetDateTime.now());
        var dto = new UserProfileResponseDTO(userAccount, 90);
        assertThat(dto.isPinConfigured()).isFalse();
    }
}
