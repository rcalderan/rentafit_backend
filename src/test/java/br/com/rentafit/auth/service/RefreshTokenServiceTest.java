package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.RefreshToken;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.RefreshTokenRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationDays", 7L);
    }

    @Test
    @DisplayName("Should create a new refresh token")
    void createRefreshToken() {
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(userId);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(userId);

        assertThat(token).isNotNull();
        assertThat(token.getUserAccount()).isEqualTo(user);
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getExpiryDate()).isAfter(Instant.now());

        verify(refreshTokenRepository).deleteByUserAccount(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should verify expiration - non expired")
    void verifyExpiration_valid() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(Instant.now().plusSeconds(3600));

        RefreshToken result = refreshTokenService.verifyExpiration(token);

        assertThat(result).isEqualTo(token);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should verify expiration - expired")
    void verifyExpiration_expired() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(Instant.now().minusSeconds(3600));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    @DisplayName("Should find token by string")
    void findByToken() {
        String tokenStr = "some-token";
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        Optional<RefreshToken> result = refreshTokenService.findByToken(tokenStr);

        assertThat(result).isPresent().contains(token);
    }

    @Test
    @DisplayName("Should delete token by user id")
    void deleteByUserId() {
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.deleteByUserAccount(user)).thenReturn(1);

        int deletedCount = refreshTokenService.deleteByUserId(userId);

        assertThat(deletedCount).isEqualTo(1);
    }
}
