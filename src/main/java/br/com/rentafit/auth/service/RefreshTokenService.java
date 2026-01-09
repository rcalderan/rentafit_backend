package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.RefreshToken;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.repository.RefreshTokenRepository;
import br.com.rentafit.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${rentafit.security.jwt.refresh-expiration-days:7}")
    private Long refreshExpirationDays;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserAccountRepository userAccountRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(UUID userId) {
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Remove old tokens
        refreshTokenRepository.deleteByUserAccount(userAccount);

        RefreshToken refreshToken = RefreshToken.builder()
                .userAccount(userAccount)
                .expiryDate(Instant.now().plusSeconds(refreshExpirationDays * 24 * 60 * 60))
                .token(UUID.randomUUID().toString())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public int deleteByUserId(UUID userId) {
        return userAccountRepository.findById(userId)
                .map(refreshTokenRepository::deleteByUserAccount)
                .orElse(0);
    }
}

