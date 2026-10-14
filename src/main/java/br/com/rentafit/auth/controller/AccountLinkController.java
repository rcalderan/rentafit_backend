package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.dto.AccountLinkTokenRequestDTO;
import br.com.rentafit.auth.dto.LoginResponseDTO;
import br.com.rentafit.auth.dto.ResendVerificationRequestDTO;
import br.com.rentafit.auth.service.AccountLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Links de conta", description = "Confirmação de e-mail e desbloqueio via link enviado por e-mail")
public class AccountLinkController {

    private final AccountLinkService accountLinkService;

    @PostMapping("/verify-email")
    @Operation(summary = "Confirma o e-mail", description = "Ativa a conta e retorna tokens para seguir ao setup de senha/PIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "E-mail confirmado"),
            @ApiResponse(responseCode = "422", description = "Link inválido, expirado ou já utilizado")
    })
    public ResponseEntity<LoginResponseDTO> verifyEmail(@Valid @RequestBody AccountLinkTokenRequestDTO request) {
        return ResponseEntity.ok(accountLinkService.activate(request.token()));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Reenvia o e-mail de confirmação",
            description = "Responde 202 sempre, exista ou não conta pendente para o e-mail (evita enumeração).")
    @ApiResponse(responseCode = "202", description = "Pedido aceito")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequestDTO request) {
        accountLinkService.resendActivation(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/unlock")
    @Operation(summary = "Desbloqueia a conta", description = "Libera uma conta bloqueada por tentativas de login inválidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conta desbloqueada"),
            @ApiResponse(responseCode = "422", description = "Link inválido, expirado ou já utilizado")
    })
    public ResponseEntity<Void> unlock(@Valid @RequestBody AccountLinkTokenRequestDTO request) {
        accountLinkService.unlock(request.token());
        return ResponseEntity.noContent().build();
    }
}
