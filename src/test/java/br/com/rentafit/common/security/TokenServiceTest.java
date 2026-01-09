package br.com.rentafit.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret-key-for-unit-testing-32-chars");
        ReflectionTestUtils.setField(tokenService, "issuer", "rentafit-api-test");
        ReflectionTestUtils.setField(tokenService, "expirationHours", 2L);
    }

    @Test
    @DisplayName("Should generate a valid JWT token")
    void generateToken() {
        String username = "testuser";
        String token = tokenService.generateToken(username);

        assertThat(token).isNotBlank();

        String validatedUser = tokenService.validateToken(token);
        assertThat(validatedUser).isEqualTo(username);
    }

    @Test
    @DisplayName("Should return null for invalid token")
    void validateToken_invalid() {
        String invalidToken = "invalid.token.value";
        String validatedUser = tokenService.validateToken(invalidToken);

        assertThat(validatedUser).isNull();
    }
}

