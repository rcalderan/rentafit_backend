package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.Role;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.domain.RoleName;
import br.com.rentafit.auth.repository.UserAccountRepository;
import br.com.rentafit.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do UserAccountService usando Mockito.
 * Testa apenas a lógica de negócio do serviço de autenticação.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAccountService - Unit Tests")
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserAccountService userAccountService;

    private UserAccount userAccount;
    private String username;

    @BeforeEach
    void setUp() throws Exception {
        userAccountService = new UserAccountService(userAccountRepository, passwordEncoder);
        Field expiryField = UserAccountService.class.getDeclaredField("passwordExpiryDays");
        expiryField.setAccessible(true);
        expiryField.set(userAccountService, 90L);

        username = "john.doe";

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setRole(RoleName.ADMIN);

        userAccount = new UserAccount();
        userAccount.setId(UUID.randomUUID());
        userAccount.setUsername(username);
        userAccount.setPassword("$2a$10$hashedPassword");
        userAccount.setRoles(List.of(adminRole));
        userAccount.setIsActive(true);
    }

    @Test
    @DisplayName("Should load user by username when user exists")
    void testLoadUserByUsername_Success() {
        // Arrange
        when(userAccountRepository.findByUsername(username)).thenReturn(Optional.of(userAccount));

        // Act
        UserDetails result = userAccountService.loadUserByUsername(username);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPassword()).isEqualTo("$2a$10$hashedPassword");
        assertThat(result.getAuthorities()).isNotEmpty();

        verify(userAccountRepository, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void testLoadUserByUsername_UserNotFound() {
        // Arrange
        String nonExistentUsername = "nonexistent.user";
        when(userAccountRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userAccountService.loadUserByUsername(nonExistentUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username: " + nonExistentUsername);

        verify(userAccountRepository, times(1)).findByUsername(nonExistentUsername);
    }

    @Test
    @DisplayName("Should return UserDetails with correct role")
    void testLoadUserByUsername_VerifyRole() {
        // Arrange
        when(userAccountRepository.findByUsername(username)).thenReturn(Optional.of(userAccount));

        // Act
        UserDetails result = userAccountService.loadUserByUsername(username);

        // Assert
        assertThat(result.getAuthorities())
                .hasSize(1)
                .extracting("authority")
                .contains("ROLE_ADMIN");

        verify(userAccountRepository, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("Should handle employee role correctly")
    void testLoadUserByUsername_EmployeeRole() {
        // Arrange
        Role employeeRole = new Role();
        employeeRole.setId(2L);
        employeeRole.setRole(RoleName.EMPLOYEE);
        userAccount.setRoles(List.of(employeeRole));

        when(userAccountRepository.findByUsername(username)).thenReturn(Optional.of(userAccount));

        // Act
        UserDetails result = userAccountService.loadUserByUsername(username);

        // Assert
        assertThat(result.getAuthorities())
                .hasSize(1)
                .extracting("authority")
                .contains("ROLE_EMPLOYEE");

        verify(userAccountRepository, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("Should handle customer role correctly")
    void testLoadUserByUsername_CustomerRole() {
        // Arrange
        Role customerRole = new Role();
        customerRole.setId(3L);
        customerRole.setRole(RoleName.CUSTOMER);
        userAccount.setRoles(List.of(customerRole));

        when(userAccountRepository.findByUsername(username)).thenReturn(Optional.of(userAccount));

        // Act
        UserDetails result = userAccountService.loadUserByUsername(username);

        // Assert
        assertThat(result.getAuthorities())
                .hasSize(1)
                .extracting("authority")
                .contains("ROLE_CUSTOMER");

        verify(userAccountRepository, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("Should get user with details when user exists")
    void testGetUserWithDetails_Success() {
        // Arrange
        when(userAccountRepository.findByUsernameWithDetails(username)).thenReturn(Optional.of(userAccount));

        // Act
        Optional<UserAccount> result = userAccountService.getUserWithDetails(username);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username);
        assertThat(result.get().getId()).isEqualTo(userAccount.getId());

        verify(userAccountRepository, times(1)).findByUsernameWithDetails(username);
    }

    @Test
    @DisplayName("Should return empty when user does not exist in getUserWithDetails")
    void testGetUserWithDetails_NotFound() {
        // Arrange
        String nonExistentUsername = "nonexistent";
        when(userAccountRepository.findByUsernameWithDetails(nonExistentUsername)).thenReturn(Optional.empty());

        // Act
        Optional<UserAccount> result = userAccountService.getUserWithDetails(nonExistentUsername);

        // Assert
        assertThat(result).isEmpty();

        verify(userAccountRepository, times(1)).findByUsernameWithDetails(nonExistentUsername);
    }

    @Test
    @DisplayName("Should find user profile when user exists")
    void testFindUserProfile_Success() {
        // Arrange
        when(userAccountRepository.findByUsernameWithDetails(username)).thenReturn(Optional.of(userAccount));

        // Act
        Optional<UserAccount> result = userAccountService.findUserProfile(username);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username);

        verify(userAccountRepository, times(1)).findByUsernameWithDetails(username);
    }

    @Test
    @DisplayName("Should return empty when user profile not found")
    void testFindUserProfile_NotFound() {
        // Arrange
        String nonExistentUsername = "nonexistent";
        when(userAccountRepository.findByUsernameWithDetails(nonExistentUsername)).thenReturn(Optional.empty());

        // Act
        Optional<UserAccount> result = userAccountService.findUserProfile(nonExistentUsername);

        // Assert
        assertThat(result).isEmpty();

        verify(userAccountRepository, times(1)).findByUsernameWithDetails(nonExistentUsername);
    }

    @Test
    @DisplayName("isEnabled deve retornar false quando isActive=false")
    void testIsEnabled_ReturnsFalseWhenInactive() {
        userAccount.setIsActive(false);
        when(userAccountRepository.findByUsername(username)).thenReturn(Optional.of(userAccount));

        UserDetails result = userAccountService.loadUserByUsername(username);

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("isEnabled deve retornar true quando isActive=true")
    void testIsEnabled_ReturnsTrueWhenActive() {
        userAccount.setIsActive(true);
        when(userAccountRepository.findByUsername(username)).thenReturn(Optional.of(userAccount));

        UserDetails result = userAccountService.loadUserByUsername(username);

        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername deve retornar conta inativa sem lançar exceção (regra Spring Security)")
    void testLoadUserByUsername_InactiveAccountReturned() {
        // Spring Security verifica isEnabled() no DaoAuthenticationProvider — não no loadUserByUsername.
        // O service deve retornar a conta e deixar o DaoAuthenticationProvider lançar DisabledException.
        userAccount.setIsActive(false);
        when(userAccountRepository.findByUsername(username)).thenReturn(Optional.of(userAccount));

        UserDetails result = userAccountService.loadUserByUsername(username);

        assertThat(result).isNotNull();
        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("getPasswordExpiryDays deve retornar o valor configurado")
    void testGetPasswordExpiryDays() {
        assertThat(userAccountService.getPasswordExpiryDays()).isEqualTo(90L);
    }

    @Nested
    @DisplayName("setupCredentials")
    class SetupCredentialsTests {

        @Test
        @DisplayName("Should set password, PIN, and passwordChangedAt on first access")
        void testSetupCredentials_Success() {
            userAccount.setPin(null);
            when(passwordEncoder.encode("NewP@ss1")).thenReturn("$2a$10$encodedHash");
            when(passwordEncoder.encode("1234")).thenReturn("$2a$10$encodedPinHash");
            when(userAccountRepository.save(userAccount)).thenReturn(userAccount);

            userAccountService.setupCredentials(userAccount, "NewP@ss1", "1234");

            assertThat(userAccount.getPassword()).isEqualTo("$2a$10$encodedHash");
            assertThat(userAccount.getPin()).isEqualTo("$2a$10$encodedPinHash");
            assertThat(userAccount.getPasswordChangedAt()).isNotNull();
            assertThat(userAccount.getPasswordChangedAt()).isBefore(OffsetDateTime.now().plusSeconds(1));
            verify(userAccountRepository).save(userAccount);
            verify(passwordEncoder).encode("NewP@ss1");
        }

        @Test
        @DisplayName("Should throw ValidationException when PIN already set")
        void testSetupCredentials_AlreadyConfigured() {
            userAccount.setPin("9999");

            assertThatThrownBy(() -> userAccountService.setupCredentials(userAccount, "NewP@ss1", "1234"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Credentials already configured");

            verify(userAccountRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(any());
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should update password and passwordChangedAt")
        void testChangePassword_Success() {
            when(passwordEncoder.encode("AnotherP@ss1")).thenReturn("$2a$10$newHash");
            when(userAccountRepository.save(userAccount)).thenReturn(userAccount);

            userAccountService.changePassword(userAccount, "AnotherP@ss1");

            assertThat(userAccount.getPassword()).isEqualTo("$2a$10$newHash");
            assertThat(userAccount.getPasswordChangedAt()).isNotNull();
            verify(userAccountRepository).save(userAccount);
            verify(passwordEncoder).encode("AnotherP@ss1");
        }

        @Test
        @DisplayName("Should update passwordChangedAt to current time")
        void testChangePassword_UpdatesTimestamp() {
            OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);
            when(passwordEncoder.encode(any())).thenReturn("hash");
            when(userAccountRepository.save(userAccount)).thenReturn(userAccount);

            userAccountService.changePassword(userAccount, "ValidP@ss1");

            assertThat(userAccount.getPasswordChangedAt()).isAfter(before);
        }
    }
}


