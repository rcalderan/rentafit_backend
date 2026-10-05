package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.common.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginSessionIssuer {
    private final TokenService tokens;
    private final RefreshTokenService refreshTokens;
    private final InstallationCompletionService installation;

    @Transactional
    public LoginResponseDTO issue(UserAccount account) {
        String accessToken = tokens.generateToken(account.getUsername());
        String refreshToken = refreshTokens.createRefreshToken(account).getToken();
        installation.afterVerifiedLogin(account);
        return new LoginResponseDTO(accessToken, refreshToken, "Bearer");
    }
}
