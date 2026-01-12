package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.domain.RefreshToken;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.dto.TokenRefreshRequestDTO;
import br.com.rentafit.auth.service.RefreshTokenService;
import br.com.rentafit.common.security.CryptoService;
import br.com.rentafit.common.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para login e gerenciamento de tokens")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final CryptoService cryptoService;

    @GetMapping("/public-key")
    @Operation(summary = "Obtém a chave pública RSA", description = "Retorna a chave pública atual para criptografia de dados sensíveis no front-end")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", cryptoService.getPublicKeyBase64()));
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza o login do usuário", description = "Retorna um access token JWT e um refresh token. A senha deve ser enviada criptografada com a chave pública RSA fornecida.")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO data) {
        String decryptedPassword = cryptoService.decrypt(data.password());

        var usernamePassword = new UsernamePasswordAuthenticationToken(data.username(), decryptedPassword);
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var user = (UserAccount) auth.getPrincipal();
        var accessToken = tokenService.generateToken(user.getUsername());
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return ResponseEntity.ok(new LoginResponseDTO(accessToken, refreshToken.getToken(), "Bearer"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o access token", description = "Utiliza o refresh token para gerar um novo access token")
    public ResponseEntity<LoginResponseDTO> refreshToken(@RequestBody @Valid TokenRefreshRequestDTO request) {
        return refreshTokenService.findByToken(request.refreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserAccount)
                .map(userAccount -> {
                    String accessToken = tokenService.generateToken(userAccount.getUsername());
                    // Rotaciona o refresh token para maior segurança
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(userAccount.getId());
                    return ResponseEntity.ok(new LoginResponseDTO(accessToken, newRefreshToken.getToken(), "Bearer"));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
