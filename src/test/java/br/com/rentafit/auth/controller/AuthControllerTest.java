package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.domain.RefreshToken;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.domain.UserRole;
import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.dto.TokenRefreshRequestDTO;
import br.com.rentafit.auth.service.RefreshTokenService;
import br.com.rentafit.common.security.CryptoService;
import br.com.rentafit.common.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Should return public key")
    void getPublicKey() {
        when(cryptoService.getPublicKeyBase64()).thenReturn("test-public-key");

        ResponseEntity<Map<String, String>> response = authController.getPublicKey();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("publicKey")).isEqualTo("test-public-key");
    }

    @Test
    @DisplayName("Should login successfully")
    void login() {
        LoginRequestDTO request = new LoginRequestDTO("user", "encrypted-pass");
        UserAccount user = new UserAccount();
        user.setUsername("user");
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.ROLE_ADMIN);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        when(cryptoService.decrypt("encrypted-pass")).thenReturn("plain-pass");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenService.generateToken("user")).thenReturn("access-token");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(refreshToken);

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
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(newToken);

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
}
