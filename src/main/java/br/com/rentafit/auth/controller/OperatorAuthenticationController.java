package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.dto.LoginRequestDTO;
import br.com.rentafit.auth.dto.OperatorLoginResponseDTO;
import br.com.rentafit.auth.dto.OperatorProfileResponseDTO;
import br.com.rentafit.auth.service.OperatorAuthenticationService;
import br.com.rentafit.auth.service.OperatorIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OperatorAuthenticationController {
    private final OperatorAuthenticationService operatorAuthenticationService;
    private final OperatorIdentityService operatorIdentityService;

    @PostMapping("/operator-login")
    @Operation(summary = "Autentica um usuário operacional sem alterar a sessão de outro usuário")
    public OperatorLoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return operatorAuthenticationService.login(request);
    }

    @GetMapping("/operator-profile")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Revalida o perfil operacional do titular do token JWT")
    public OperatorProfileResponseDTO profile(Authentication authentication) {
        return operatorIdentityService.profile(authentication.getName());
    }
}
