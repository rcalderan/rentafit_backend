package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.domain.RefreshToken;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.dto.TokenRefreshRequestDTO;
import br.com.rentafit.auth.dto.SetupCredentialsRequestDTO;
import br.com.rentafit.auth.dto.SetupIssuerCnpjRequestDTO;
import br.com.rentafit.auth.dto.ChangePasswordRequestDTO;
import br.com.rentafit.auth.dto.UpdateUserPreferencesRequestDTO;
import br.com.rentafit.auth.dto.UserProfileResponseDTO;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import br.com.rentafit.auth.exception.InvalidCredentialsException;
import br.com.rentafit.auth.service.PasswordLoginService;
import br.com.rentafit.auth.service.RefreshTokenService;
import br.com.rentafit.auth.service.UserAccountService;
import br.com.rentafit.common.security.CryptoService;
import br.com.rentafit.common.security.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Mock
    private PasswordLoginService passwordLoginService;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CryptoService cryptoService;

    @Mock
    private UserAccountService userAccountService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Should return public key when enabled")
    void getPublicKeyEnabled() {
        when(cryptoService.isRsaEnabled()).thenReturn(true);
        when(cryptoService.getPublicKeyBase64()).thenReturn("test-public-key");

        ResponseEntity<Map<String, String>> response = authController.getPublicKey();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("publicKey")).isEqualTo("test-public-key");
    }

    @Test
    @DisplayName("Should return 403 when public key is disabled")
    void getPublicKeyDisabled() {
        when(cryptoService.isRsaEnabled()).thenReturn(false);

        ResponseEntity<Map<String, String>> response = authController.getPublicKey();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("login delega ao PasswordLoginService e retorna 200 com tokens")
    void login() {
        LoginRequestDTO request = new LoginRequestDTO("user", "plain-pass");
        when(passwordLoginService.login(request))
                .thenReturn(new LoginResponseDTO("access-token", "refresh-token", "Bearer"));

        ResponseEntity<LoginResponseDTO> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("login propaga InvalidCredentialsException para o AuthExceptionHandler")
    void login_InvalidCredentialsPropagates() {
        LoginRequestDTO request = new LoginRequestDTO("user", "wrong-password");
        when(passwordLoginService.login(request)).thenThrow(new InvalidCredentialsException(3));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void refreshToken() {
        TokenRefreshRequestDTO request = new TokenRefreshRequestDTO("old-refresh-token");
        UserAccount user = new UserAccount();
        user.setUsername("user");
        user.setId(UUID.randomUUID());

        RefreshToken oldToken = new RefreshToken();
        oldToken.setUserAccount(user);

        RefreshToken newToken = new RefreshToken();
        newToken.setToken("new-refresh-token");

        when(refreshTokenService.findByToken("old-refresh-token")).thenReturn(Optional.of(oldToken));
        when(refreshTokenService.verifyExpiration(oldToken)).thenReturn(oldToken);
        when(tokenService.generateToken("user")).thenReturn("new-access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(newToken);

        ResponseEntity<LoginResponseDTO> response = authController.refreshToken(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("new-access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("Should throw exception when refresh token not found")
    void refreshToken_NotFound() {
        TokenRefreshRequestDTO request = new TokenRefreshRequestDTO("non-existent");

        when(refreshTokenService.findByToken("non-existent")).thenReturn(Optional.empty());

        try {
            authController.refreshToken(request);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Refresh token is not in database!");
        }
    }

    @Test
    @DisplayName("Should setup credentials successfully on first access")
    void setupCredentials_Success() {
        UserAccount principal = new UserAccount();
        principal.setUsername("user");
        principal.setPin(null);

        SetupCredentialsRequestDTO request = new SetupCredentialsRequestDTO("NewP@ssw0rd!", "4831");

        ResponseEntity<Void> response = authController.setupCredentials(principal, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccountService).setupCredentials(principal, "NewP@ssw0rd!", "4831");
    }

    @Test
    @DisplayName("Should setup issuer CNPJ successfully")
    void setupIssuerCnpj_Success() {
        UserAccount principal = new UserAccount();
        principal.setUsername("user");
        principal.setId(UUID.randomUUID());

        UserAccount updated = new UserAccount();
        updated.setUsername("user");
        updated.setId(principal.getId());
        updated.setIssuerCnpj("08299621000120");
        when(userAccountService.setupIssuerCnpj(principal, "08299621000120")).thenReturn(updated);

        SetupIssuerCnpjRequestDTO request = new SetupIssuerCnpjRequestDTO("08299621000120");

        ResponseEntity<UserProfileResponseDTO> response = authController.setupIssuerCnpj(principal, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIssuerCnpj()).isEqualTo("08299621000120");
        verify(userAccountService).setupIssuerCnpj(principal, "08299621000120");
    }

    @Test
    @DisplayName("Should update user preferences successfully")
    void updatePreferences_Success() {
        UserAccount principal = new UserAccount();
        principal.setUsername("user");
        principal.setId(UUID.randomUUID());

        UserAccount updated = new UserAccount();
        updated.setUsername("user");
        updated.setId(principal.getId());
        updated.setUiVariant("legacy");
        updated.setSidebarVisible(false);
        when(userAccountService.updatePreferences(principal, "legacy", false)).thenReturn(updated);

        UpdateUserPreferencesRequestDTO request = new UpdateUserPreferencesRequestDTO("legacy", false);

        ResponseEntity<UserProfileResponseDTO> response = authController.updatePreferences(principal, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUiVariant()).isEqualTo("legacy");
        assertThat(response.getBody().isSidebarVisible()).isFalse();
        verify(userAccountService).updatePreferences(principal, "legacy", false);
    }

    @Test
    @DisplayName("Should change password successfully")
    void changePassword_Success() {
        UserAccount principal = new UserAccount();
        principal.setUsername("user");

        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("AnotherP@ss1");

        ResponseEntity<Void> response = authController.changePassword(principal, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccountService).changePassword(principal, "AnotherP@ss1");
    }

    @Test
    @DisplayName("Should return user profile when authenticated user is found")
    void getCurrentUser_Success() {
        UserAccount user = new UserAccount();
        user.setUsername("user");
        user.setId(UUID.randomUUID());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userAccountService.getUserWithDetails("user")).thenReturn(Optional.of(user));
        when(userAccountService.getPasswordExpiryDays()).thenReturn(90L);

        ResponseEntity<UserProfileResponseDTO> response = authController.getCurrentUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("user");
    }

    @Test
    @DisplayName("Should return 404 when authenticated user is not found")
    void getCurrentUser_NotFound() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("non-existent");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userAccountService.getUserWithDetails("non-existent")).thenReturn(Optional.empty());

        ResponseEntity<UserProfileResponseDTO> response = authController.getCurrentUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return 500 when getCurrentUser throws an exception")
    void getCurrentUser_Exception() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userAccountService.getUserWithDetails("user")).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<UserProfileResponseDTO> response = authController.getCurrentUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}





