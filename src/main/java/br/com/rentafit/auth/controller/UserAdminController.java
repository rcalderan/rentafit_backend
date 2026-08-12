package br.com.rentafit.auth.controller;

import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.dto.UpdateUserRoleRequestDTO;
import br.com.rentafit.auth.dto.UserSummaryDTO;
import br.com.rentafit.auth.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
@Tag(name = "Administração de Usuários", description = "Listagem e elevação/remoção de privilégio de usuários")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class UserAdminController {

    private final UserRoleService userRoleService;

    @GetMapping
    @Operation(summary = "Lista usuários", description = "Retorna usuários paginados para administração")
    @ApiResponse(responseCode = "200", description = "Usuários retornados com sucesso")
    public ResponseEntity<Page<UserSummaryDTO>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(userRoleService.listUsers(pageable));
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Altera o papel de um usuário",
            description = "Eleva ou remove privilégio respeitando a hierarquia ADMIN > MANAGER > EMPLOYEE > CUSTOMER")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Papel atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "422", description = "Regra de hierarquia violada")
    })
    public ResponseEntity<UserSummaryDTO> updateRole(
            @AuthenticationPrincipal UserAccount actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequestDTO request) {
        return ResponseEntity.ok(userRoleService.setRole(actor, id, request.role()));
    }
}
