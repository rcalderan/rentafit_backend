package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.domain.RefreshToken;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.dto.TokenRefreshRequestDTO;
import br.com.rentafit.auth.dto.SetupCredentialsRequestDTO;
import br.com.rentafit.auth.dto.SetupIssuerCnpjRequestDTO;
import br.com.rentafit.auth.dto.ChangePasswordRequestDTO;
import br.com.rentafit.auth.dto.UserProfileResponseDTO;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private AuthenticationManager authenticationManager;

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
    @DisplayName("Should login successfully without RSA")
    void login() {
        LoginRequestDTO request = new LoginRequestDTO("user", "plain-pass");
        UserAccount user = new UserAccount();
        user.setUsername("user");
        user.setId(UUID.randomUUID());

        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenService.generateToken("user")).thenReturn("access-token");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        ResponseEntity<LoginResponseDTO> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token");
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
    @DisplayName("Should return 403 when login with BCrypt hash password")
    void login_WithBCryptHash() {
        // BCrypt hash example
        LoginRequestDTO request = new LoginRequestDTO("user", "$2a$10$abcdefghijklmnopqrstuvwxyz123456789012345678901234");

        // Act
        ResponseEntity<LoginResponseDTO> response = authController.login(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should return 403 when authentication fails")
    void login_AuthenticationFailure() {
        LoginRequestDTO request = new LoginRequestDTO("user", "wrong-password");

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act
        ResponseEntity<LoginResponseDTO> response = authController.login(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should setup credentials successfully on first access")
    void setupCredentials_Success() {
        UserAccount principal = new UserAccount();
        principal.setUsername("user");
        principal.setPin(null);

        SetupCredentialsRequestDTO request = new SetupCredentialsRequestDTO("NewP@ss1", "1234");

        ResponseEntity<Void> response = authController.setupCredentials(principal, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccountService).setupCredentials(principal, "NewP@ss1", "1234");
    }

    @Test
    @DisplayName("Should setup issuer CNPJ successfully")
    void setupIssuerCnpj_Success() {
        UserAccount principal = new UserAccount();
        principal.setUsername("user");

        SetupIssuerCnpjRequestDTO request = new SetupIssuerCnpjRequestDTO("08299621000120");

        ResponseEntity<UserProfileResponseDTO> response = authController.setupIssuerCnpj(principal, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccountService).setupIssuerCnpj(principal, "08299621000120");
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





