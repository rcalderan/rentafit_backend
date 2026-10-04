package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.domain.RefreshToken;
import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.OperatorProfileResponseDTO;
import br.com.rentafit.auth.dto.UserProfileResponseDTO;
import br.com.rentafit.common.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class OperatorAuthenticationServiceTest {
    @Mock private AuthenticationManager authenticationManager;
    @Mock private OperatorIdentityService operatorIdentityService;
    @Mock private TokenService tokenService;
    @Mock private RefreshTokenService refreshTokenService;
    @InjectMocks private OperatorAuthenticationService service;
    private UserAccount account;
    private final LoginRequestDTO request = new LoginRequestDTO("operator", "test-password");

    @BeforeEach
    void setup() {
        account = new UserAccount();
        account.setUsername("operator");
    }

    private void authenticated() {
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(account, null, account.getAuthorities()));
    }

    @Test
    void validatesCandidateBeforeIssuingTokens() {
        authenticated();
        var profile = new OperatorProfileResponseDTO(new UserProfileResponseDTO(account), "OP");
        when(operatorIdentityService.profile("operator")).thenReturn(profile);
        when(tokenService.generateToken("operator")).thenReturn("test-access");
        var refresh = new RefreshToken();
        refresh.setToken("test-refresh");
        when(refreshTokenService.createRefreshToken(account)).thenReturn(refresh);

        var response = service.login(request);

        assertThat(response.profile()).isEqualTo(profile);
        assertThat(response.accessToken()).isEqualTo("test-access");
        var order = inOrder(authenticationManager, operatorIdentityService, tokenService, refreshTokenService);
        order.verify(authenticationManager).authenticate(any());
        order.verify(operatorIdentityService).profile("operator");
        order.verify(tokenService).generateToken("operator");
        order.verify(refreshTokenService).createRefreshToken(account);
    }

    @Test
    void deniedRoleDoesNotIssueOrRevokeTokens() {
        authenticated();
        when(operatorIdentityService.profile("operator")).thenThrow(new AccessDeniedException("Denied"));

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(tokenService, refreshTokenService);
    }

    @Test
    void invalidPasswordDoesNotIssueTokens() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(operatorIdentityService, tokenService, refreshTokenService);
    }

    @Test
    void inactiveCandidateDoesNotIssueTokens() {
        account.setIsActive(false);
        authenticated();
        assertThatThrownBy(() -> service.login(request)).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(operatorIdentityService, tokenService, refreshTokenService);
    }

    @Test
    void persistenceFailureDoesNotReturnTokens() {
        authenticated();
        when(operatorIdentityService.profile("operator"))
                .thenReturn(new OperatorProfileResponseDTO(new UserProfileResponseDTO(account), "OP"));
        when(tokenService.generateToken("operator")).thenReturn("test-access");
        when(refreshTokenService.createRefreshToken(account)).thenThrow(new IllegalStateException("Persistence failed"));

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(IllegalStateException.class);
    }
}
