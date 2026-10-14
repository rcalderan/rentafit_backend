package br.com.rentafit.auth.repository;

import br.com.rentafit.auth.domain.AccountTokenPurpose;
import br.com.rentafit.auth.domain.AccountVerificationToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AccountVerificationTokenRepository extends JpaRepository<AccountVerificationToken, UUID> {

    @EntityGraph(attributePaths = {"userAccount"})
    Optional<AccountVerificationToken> findByTokenHashAndPurpose(String tokenHash, AccountTokenPurpose purpose);

    /** Invalidates older links when a new one is issued, so only the latest e-mail works. */
    @Modifying
    @Query("""
            UPDATE AccountVerificationToken token SET token.usedAt = :now
            WHERE token.userAccount.id = :userId AND token.purpose = :purpose AND token.usedAt IS NULL
            """)
    int revokeActive(@Param("userId") UUID userId, @Param("purpose") AccountTokenPurpose purpose,
                     @Param("now") OffsetDateTime now);
}
