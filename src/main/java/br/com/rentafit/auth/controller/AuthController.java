package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.domain.RefreshToken;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.ChangePasswordRequestDTO;
import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.dto.SetupCredentialsRequestDTO;
import br.com.rentafit.auth.dto.SetupIssuerCnpjRequestDTO;
import br.com.rentafit.auth.dto.TokenRefreshRequestDTO;
import br.com.rentafit.auth.dto.UserProfileResponseDTO;
import br.com.rentafit.auth.service.PasswordLoginService;
import br.com.rentafit.auth.service.RefreshTokenService;
import br.com.rentafit.auth.service.UserAccountService;
import br.com.rentafit.common.security.CryptoService;
import br.com.rentafit.common.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticação", description = "Endpoints para login e gerenciamento de tokens")
public class AuthController {

    private final PasswordLoginService passwordLoginService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final CryptoService cryptoService;
    private final UserAccountService userAccountService;

    @GetMapping("/public-key")
    @Operation(summary = "Obtém a chave pública RSA", description = "Retorna a chave pública atual para criptografia de dados sensíveis no front-end")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        if (!cryptoService.isRsaEnabled()) {
            return ResponseEntity.status(403).build();
        }
        var publicKey = cryptoService.getPublicKeyBase64();
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza o login do usuário", description = "Retorna um access token JWT e um refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado"),
        @ApiResponse(responseCode = "403", description = "INVALID_CREDENTIALS (details.remainingAttempts) ou EMAIL_NOT_VERIFIED"),
        @ApiResponse(responseCode = "423", description = "ACCOUNT_LOCKED (details.lockedUntil)")
    })
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO data) {
        return ResponseEntity.ok(passwordLoginService.login(data));
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
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(userAccount);
                    return ResponseEntity.ok(new LoginResponseDTO(accessToken, newRefreshToken.getToken(), "Bearer"));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtém o perfil do usuário autenticado", 
              description = "Retorna os dados completos do usuário logado baseado no token JWT")
    public ResponseEntity<UserProfileResponseDTO> getCurrentUser() {
        try{
            String username = SecurityContextHolder.getContext()
                    .getAuthentication().getName();

            return userAccountService.getUserWithDetails(username)
                    .map(user -> new UserProfileResponseDTO(user, userAccountService.getPasswordExpiryDays()))
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Erro ao buscar perfil do usuário", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/setup-credentials")
    @Operation(summary = "Setup inicial de credenciais",
              description = "Define senha e PIN no primeiro acesso. Requer que o PIN ainda não esteja configurado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Credentials configured successfully"),
        @ApiResponse(responseCode = "422", description = "Validation error or credentials already configured")
    })
    public ResponseEntity<Void> setupCredentials(
            @AuthenticationPrincipal UserAccount principal,
            @Valid @RequestBody SetupCredentialsRequestDTO request) {
        userAccountService.setupCredentials(principal, request.newPassword(), request.pin());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Alterar senha expirada",
              description = "Permite ao usuário autenticado definir uma nova senha quando expirada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "422", description = "Validation error")
    })
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserAccount principal,
            @Valid @RequestBody ChangePasswordRequestDTO request) {
        userAccountService.changePassword(principal, request.newPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/setup-issuer-cnpj")
    @Operation(summary = "Vincular CNPJ do emitente",
              description = "Vincula o usuário autenticado ao CNPJ do emitente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CNPJ vinculado com sucesso"),
        @ApiResponse(responseCode = "422", description = "Validation error")
    })
    public ResponseEntity<UserProfileResponseDTO> setupIssuerCnpj(
            @AuthenticationPrincipal UserAccount principal,
            @Valid @RequestBody SetupIssuerCnpjRequestDTO request) {
        UserAccount updated = userAccountService.setupIssuerCnpj(principal, request.issuerCnpj());
        return ResponseEntity.ok(new UserProfileResponseDTO(updated, userAccountService.getPasswordExpiryDays()));
    }

    // Used by Nginx auth_request to validate JWT tokens for the costume-rental-nfe gateway
    @GetMapping("/validate-token")
    @Operation(summary = "Valida o token JWT",
              description = "Retorna 200 se o Bearer token é válido. Usado pelo Nginx auth_request para proteger o microsserviço NFe.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token válido"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<Void> validateToken() {
        // Se chegou aqui, o SecurityFilter já validou o JWT com sucesso
        return ResponseEntity.ok().build();
    }
}
