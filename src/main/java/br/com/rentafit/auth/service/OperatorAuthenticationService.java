package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.OperatorLoginResponseDTO;
import br.com.rentafit.common.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperatorAuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final OperatorIdentityService operatorIdentityService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    public OperatorLoginResponseDTO login(LoginRequestDTO request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        if (!(authentication.getPrincipal() instanceof UserAccount account)
                || !Boolean.TRUE.equals(account.getIsActive())) {
            throw new BadCredentialsException("Credenciais inválidas.");
        }
        var profile = operatorIdentityService.profile(account.getUsername());
        var accessToken = tokenService.generateToken(account.getUsername());
        var refreshToken = refreshTokenService.createRefreshToken(account);
        return new OperatorLoginResponseDTO(accessToken, refreshToken.getToken(), "Bearer", profile);
    }
}
