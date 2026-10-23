package br.com.rentafit.auth.service;

import br.com.rentafit.auth.config.AccountLinkProperties;
import br.com.rentafit.auth.domain.AccountTokenPurpose;
import br.com.rentafit.auth.event.AccountLinkMailRequestedEvent;
import br.com.rentafit.common.mail.MailGateway;
import br.com.rentafit.common.mail.OutgoingMail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("AccountLinkMailer")
class AccountLinkMailerTest {

    /** In-memory MailGateway that records messages, optionally failing like an SMTP outage. */
    static class FakeMailGateway implements MailGateway {
        final List<OutgoingMail> sent = new ArrayList<>();
        boolean failing;

        @Override
        public void send(OutgoingMail mail) {
            if (failing) {
                throw new MailSendException("SMTP indisponível");
            }
            sent.add(mail);
        }
    }

    private static final OffsetDateTime VALID_UNTIL = OffsetDateTime.parse("2026-10-12T15:30:00Z");

    FakeMailGateway gateway;
    AccountLinkMailer mailer;

    @BeforeEach
    void setUp() {
        AccountLinkProperties properties = new AccountLinkProperties();
        properties.setFrontendBaseUrl("https://noivamodas.com.br");
        gateway = new FakeMailGateway();
        mailer = new AccountLinkMailer(gateway, properties);
    }

    private AccountLinkMailRequestedEvent event(AccountTokenPurpose purpose) {
        return new AccountLinkMailRequestedEvent(purpose, "ana@ex.com", "Ana", "raw-token-123", VALID_UNTIL);
    }

    @Test
    @DisplayName("e-mail de ativação contém o link /auth/verify-email com o token e o prazo em horário de Brasília")
    void activationMail() {
        mailer.onLinkRequested(event(AccountTokenPurpose.ACTIVATION));

        OutgoingMail mail = gateway.sent.getFirst();
        assertThat(mail.to()).isEqualTo("ana@ex.com");
        assertThat(mail.subject()).isEqualTo("Confirme seu e-mail");
        assertThat(mail.body()).contains("https://noivamodas.com.br/auth/verify-email?token=raw-token-123")
                .contains("12/10/2026 às 12:30");
        assertThat(mail.toString()).doesNotContain("raw-token-123").contains("a***@ex.com");
    }

    @Test
    @DisplayName("e-mail de bloqueio contém o link /auth/unlock")
    void unlockMail() {
        mailer.onLinkRequested(event(AccountTokenPurpose.UNLOCK));

        assertThat(gateway.sent.getFirst().subject()).isEqualTo("Sua conta foi bloqueada");
        assertThat(gateway.sent.getFirst().body()).contains("https://noivamodas.com.br/auth/unlock?token=raw-token-123");
    }

    @Test
    @DisplayName("falha de SMTP é registrada sem propagar")
    void smtpFailureDoesNotPropagate() {
        gateway.failing = true;

        assertThatCode(() -> mailer.onLinkRequested(event(AccountTokenPurpose.ACTIVATION))).doesNotThrowAnyException();
        assertThat(gateway.sent).isEmpty();
    }
}
