package br.com.rentafit.auth.service;

import br.com.rentafit.auth.config.AccountLinkProperties;
import br.com.rentafit.auth.domain.AccountTokenPurpose;
import br.com.rentafit.auth.domain.AccountVerificationToken;
import br.com.rentafit.auth.domain.UserAccount;
import br.com.rentafit.auth.event.AccountLinkMailRequestedEvent;
import br.com.rentafit.auth.repository.AccountVerificationTokenRepository;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.domain.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLinkIssuer")
class AccountLinkIssuerTest {

    private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");
    private static final OffsetDateTime NOW_OFFSET = NOW.atOffset(ZoneOffset.UTC);

    @Mock
    AccountVerificationTokenRepository tokens;

    @Mock
    ApplicationEventPublisher events;

    AccountLinkIssuer issuer;
    UserAccount account;

    @BeforeEach
    void setUp() {
        issuer = new AccountLinkIssuer(tokens, new AccountLinkProperties(), events, Clock.fixed(NOW, ZoneOffset.UTC));
        Customer person = new Customer();
        person.setName("Ana");
        person.setEmail("ana@ex.com");
        account = new UserAccount();
        account.setId(UUID.randomUUID());
        account.setPerson(person);
    }

    @Test
    @DisplayName("persiste apenas o hash SHA-256, revoga links anteriores e publica o token bruto no evento")
    void issueStoresHashAndPublishesRawToken() {
        issuer.issue(account, AccountTokenPurpose.ACTIVATION);

        ArgumentCaptor<AccountVerificationToken> saved = ArgumentCaptor.forClass(AccountVerificationToken.class);
        ArgumentCaptor<AccountLinkMailRequestedEvent> event = ArgumentCaptor.forClass(AccountLinkMailRequestedEvent.class);
        verify(tokens).revokeActive(account.getId(), AccountTokenPurpose.ACTIVATION, NOW_OFFSET);
        verify(tokens).save(saved.capture());
        verify(events).publishEvent(event.capture());

        String rawToken = event.getValue().rawToken();
        assertThat(rawToken).matches("[A-Za-z0-9_-]{43}");
        assertThat(saved.getValue().getTokenHash()).isEqualTo(AccountLinkIssuer.sha256(rawToken)).isNotEqualTo(rawToken);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW_OFFSET.plusHours(24));
        assertThat(event.getValue().recipientEmail()).isEqualTo("ana@ex.com");
        assertThat(event.getValue().toString()).doesNotContain(rawToken);
    }

    @Test
    @DisplayName("link de desbloqueio expira em 30 minutos")
    void unlockLinkTtl() {
        issuer.issue(account, AccountTokenPurpose.UNLOCK);

        ArgumentCaptor<AccountVerificationToken> saved = ArgumentCaptor.forClass(AccountVerificationToken.class);
        verify(tokens).save(saved.capture());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW_OFFSET.plusMinutes(30));
    }

    @Test
    @DisplayName("conta sem e-mail não gera token nem evento")
    void accountWithoutEmailIsSkipped() {
        account.getPerson().setEmail(" ");

        issuer.issue(account, AccountTokenPurpose.UNLOCK);

        verify(tokens, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    @DisplayName("redeem marca o token como usado e devolve a conta")
    void redeemMarksTokenUsed() {
        AccountVerificationToken token = AccountVerificationToken.builder().userAccount(account)
                .purpose(AccountTokenPurpose.UNLOCK).expiresAt(NOW_OFFSET.plusMinutes(5)).build();
        when(tokens.findByTokenHashAndPurpose(AccountLinkIssuer.sha256("raw-token"), AccountTokenPurpose.UNLOCK))
                .thenReturn(Optional.of(token));

        assertThat(issuer.redeem("raw-token", AccountTokenPurpose.UNLOCK)).isSameAs(account);
        assertThat(token.getUsedAt()).isEqualTo(NOW_OFFSET);
    }

    @Test
    @DisplayName("redeem rejeita token expirado, usado ou inexistente")
    void redeemRejectsUnusableTokens() {
        AccountVerificationToken expired = AccountVerificationToken.builder().userAccount(account)
                .purpose(AccountTokenPurpose.ACTIVATION).expiresAt(NOW_OFFSET.minusSeconds(1)).build();
        when(tokens.findByTokenHashAndPurpose(anyString(), eq(AccountTokenPurpose.ACTIVATION)))
                .thenReturn(Optional.of(expired), Optional.empty());

        assertThatThrownBy(() -> issuer.redeem("expired", AccountTokenPurpose.ACTIVATION))
                .isInstanceOf(ValidationException.class).hasMessageContaining("ativação");
        assertThatThrownBy(() -> issuer.redeem("missing", AccountTokenPurpose.ACTIVATION))
                .isInstanceOf(ValidationException.class);
    }
}
