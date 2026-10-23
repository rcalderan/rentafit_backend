package br.com.rentafit.people.controller;

import br.com.rentafit.people.dto.SignUpRequestDTO;
import br.com.rentafit.people.dto.SignUpResponseDTO;
import br.com.rentafit.people.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Sign-up", description = "Public self-registration endpoint")
public class PublicRegistrationController {

    private final UserRegistrationService userRegistrationService;

    @PostMapping("/signup")
    @Operation(summary = "Self-register as a new customer",
            description = "Creates a Customer + UserAccount (CUSTOMER role) pending e-mail confirmation and "
                    + "e-mails the activation link. After confirming, the user completes password/PIN setup "
                    + "at /auth/setup-credentials.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Account created; activation e-mail sent"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "422", description = "Username, email or document already in use")
    })
    public ResponseEntity<SignUpResponseDTO> signUp(@Valid @RequestBody SignUpRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(userRegistrationService.registerCustomer(dto));
    }
}
