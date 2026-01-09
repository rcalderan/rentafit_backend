package br.com.rentafit.auth.repository;

import br.com.rentafit.auth.domain.RefreshToken;
import br.com.rentafit.auth.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    int deleteByUserAccount(UserAccount userAccount);
}

