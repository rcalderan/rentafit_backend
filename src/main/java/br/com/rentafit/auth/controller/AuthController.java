package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.domain.RefreshToken;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.dto.TokenRefreshRequestDTO;
import br.com.rentafit.auth.dto.UserProfileResponseDTO;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para login e gerenciamento de tokens")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final CryptoService cryptoService;

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    @GetMapping("/public-key")
    @Operation(summary = "Obtém a chave pública RSA", description = "Retorna a chave pública atual para criptografia de dados sensíveis no front-end")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        if (!cryptoService.isRsaEnabled()) {
            return ResponseEntity.status(403).build();
        }
        var publicKey = cryptoService.getPublicKeyBase64();
        System.out.println("Chave pública RSA fornecida: " + publicKey);
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }

    @PostMapping("/login")
    @Transactional
    @Operation(summary = "Realiza o login do usuário", description = "Retorna um access token JWT e um refresh token.")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO data) {

        try{
            System.out.println("=== Tentativa de Login ===");
            System.out.println("Username: " + data.username());
            System.out.println("Password recebido: " + data.password().substring(0, Math.min(20, data.password().length())) + "...");
            if (BCRYPT_PATTERN.matcher(data.password()).matches()) {
                System.out.println("AVISO: Senha recebida já está em formato BCrypt hash. Login negado por segurança.");
                return ResponseEntity.status(403).build();
            }
            var autenticationToken = new UsernamePasswordAuthenticationToken(data.username(), data.password());
            var authentication = authenticationManager.authenticate(autenticationToken);

            var user = (UserAccount)authentication.getPrincipal();
            System.out.println("Usuário autenticado: " + user.getUsername());
            System.out.println("Roles do usuário: " + user.getAuthorities());

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
            System.out.println("Erro na autenticação: " + e.getMessage());
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
        UserAccount user = (UserAccount) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        
        UserProfileResponseDTO response = new UserProfileResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getPerson() != null ? user.getPerson().getEmail() : null,
                user.getPerson() != null ? user.getPerson().getName() : null,
                user.getRole(),
                user.getIsActive(),
                user.getPerson() != null ? user.getPerson().getCreatedAt() : null
        );
        
        return ResponseEntity.ok(response);
    }
}
