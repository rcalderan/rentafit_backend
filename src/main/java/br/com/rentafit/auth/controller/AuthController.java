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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticação", description = "Endpoints para login e gerenciamento de tokens")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final CryptoService cryptoService;
    private final UserAccountService userAccountService;


    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

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
    @Transactional
    @Operation(summary = "Realiza o login do usuário", description = "Retorna um access token JWT e um refresh token.")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO data) {

        try{
            log.info("Tentativa de login para username: {}", data.username());
            if (BCRYPT_PATTERN.matcher(data.password()).matches()) {
                log.warn("Login negado: senha recebida em formato BCrypt hash para username: {}", data.username());
                return ResponseEntity.status(403).build();
            }
            var autenticationToken = new UsernamePasswordAuthenticationToken(data.username(), data.password());
            var authentication = authenticationManager.authenticate(autenticationToken);

            var user = (UserAccount)authentication.getPrincipal();
            log.info("Usuário autenticado: {} com roles: {}", user.getUsername(), user.getAuthorities());

            var accessToken = tokenService.generateToken(user.getUsername());
            var refreshToken = refreshTokenService.createRefreshToken(user);

            return ResponseEntity.ok(new LoginResponseDTO(accessToken, refreshToken.getToken(), "Bearer"));
//            String incomingPassword = data.password();
//            String decryptedPassword = cryptoService.decrypt(data.password());
//            if (cryptoService.isRsaEnabled()) {
//                try {
//                    incomingPassword = cryptoService.decrypt(incomingPassword);
//                } catch (Exception ex) {
//                    // Falha ao descriptografar: mantém a senha original para compatibilidade
//                    System.out.println("Falha ao descriptografar senha RSA: " + ex.getMessage());
//                }
//            }
//
//            var usernamePassword = new UsernamePasswordAuthenticationToken(data.username(), decryptedPassword);
//            var auth = this.authenticationManager.authenticate(usernamePassword);
//
//            var user = (UserAccount) auth.getPrincipal();
//            var accessToken = tokenService.generateToken(user.getUsername());
//            var refreshToken = refreshTokenService.createRefreshToken(user.getId());
//
//            return ResponseEntity.ok(new LoginResponseDTO(accessToken, refreshToken.getToken(), "Bearer"));
//            String incomingPassword = data.password();
//
//            // Se o RSA estiver habilitado, tenta descriptografar a senha recebida do front-end
//            if (cryptoService.isRsaEnabled()) {
//                try {
//                    incomingPassword = cryptoService.decrypt(incomingPassword);
//                } catch (Exception ex) {
//                    // Falha ao descriptografar: mantém a senha original para compatibilidade
//                    System.out.println("Falha ao descriptografar senha RSA: " + ex.getMessage());
//                }
//            }
//
//            // Se o cliente enviar a senha já criptografada em BCrypt, faz um fallback seguro:
//            // carrega o usuário e compara diretamente os hashes (útil para migrações/legados)
//            if (BCRYPT_PATTERN.matcher(incomingPassword).matches()) {
//                var userOpt = userAccountRepository.findByUsername(data.username());
//                if (userOpt.isPresent()) {
//                    var user = userOpt.get();
//                    if (Boolean.TRUE.equals(user.getIsActive()) && incomingPassword.equals(user.getPassword())) {
//                        String accessToken = tokenService.generateToken(user.getUsername());
//                        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());
//                        return ResponseEntity.ok(new LoginResponseDTO(accessToken, newRefreshToken.getToken(), "Bearer"));
//                    }
//                }
//                // Se o fallback não funcionar, segue para fluxo padrão e deixará falhar
//            }
//
//            // Fluxo padrão: autenticação com senha em texto puro (raw)
//            var usernamePassword = new UsernamePasswordAuthenticationToken(data.username(), incomingPassword);
//            var auth = this.authenticationManager.authenticate(usernamePassword);
//
//            var user = (UserAccount) auth.getPrincipal();
//            var accessToken = tokenService.generateToken(user.getUsername());
//            var refreshToken = refreshTokenService.createRefreshToken(user.getId());
//
//            return ResponseEntity.ok(new LoginResponseDTO(accessToken, refreshToken.getToken(), "Bearer"));

        } catch (Exception e) {
            log.warn("Falha na autenticação para username: {} - {}", data.username(), e.getMessage());
            return ResponseEntity.status(403).build();
        }
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
