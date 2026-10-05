package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class CurrentAccountIdTest {
    private final CurrentAccountId current = new CurrentAccountId();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usesOnlyAuthenticatedAccountIdentity() {
        UserAccount account = new UserAccount();
        account.setId(UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(account, null, account.getAuthorities()));
        assertThat(current.requireId()).isEqualTo(account.getId());
    }

    @Test
    void rejectsMissingOrUntrustedPrincipal() {
        assertThatThrownBy(current::requireId).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("untrusted", null));
        assertThatThrownBy(current::requireId).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
