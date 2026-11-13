package br.com.rentafit.people.controller;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.dto.SignUpRequestDTO;
import br.com.rentafit.people.dto.SignUpResponseDTO;
import br.com.rentafit.people.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Sign-up", description = "Public self-registration endpoint")
public class PublicRegistrationController {

    private static final int MAX_SIGNUPS_PER_WINDOW = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private final UserRegistrationService userRegistrationService;
    private final Map<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

    @PostMapping("/signup")
    @Operation(summary = "Self-register as a new customer",
            description = "Creates a Customer + UserAccount (CUSTOMER role) pending e-mail confirmation and "
                    + "e-mails the activation link. After confirming, the user completes password/PIN setup "
                    + "at /auth/setup-credentials.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Account created; activation e-mail sent"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "422", description = "Username, email or document already in use"),
            @ApiResponse(responseCode = "429", description = "Too many registration attempts")
    })
    public ResponseEntity<SignUpResponseDTO> signUp(@Valid @RequestBody SignUpRequestDTO dto,
                                                    HttpServletRequest request) {
        String clientKey = request != null && request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        String docKey = dto.document() != null ? dto.document().replaceAll("\\D", "") : "";
        if (!allowAttempt("ip:" + clientKey, MAX_SIGNUPS_PER_WINDOW)
                || (!docKey.isEmpty() && !allowAttempt("doc:" + docKey, 2))) {
            throw new ValidationException("Muitas tentativas de cadastro em sequência. Aguarde 1 minuto e tente novamente.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(userRegistrationService.registerCustomer(dto));
    }

    private boolean allowAttempt(String key, int maxAllowed) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(RATE_LIMIT_WINDOW);
        Deque<Instant> timestamps = attemptsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxAllowed) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
