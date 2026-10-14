package br.com.rentafit.auth.service;

import br.com.rentafit.auth.config.AccountLinkProperties;
import br.com.rentafit.auth.domain.AccountTokenPurpose;
import br.com.rentafit.auth.event.AccountLinkMailRequestedEvent;
import br.com.rentafit.common.mail.MailGateway;
import br.com.rentafit.common.mail.MailGatewayConfig;
import br.com.rentafit.common.mail.OutgoingMail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Sends activation/unlock links only after the issuing transaction commits, so an SMTP outage
 * never rolls back a sign-up or a lock, and a rolled-back transaction never leaks a link.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountLinkMailer {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final ZoneId BRAZIL = ZoneId.of("America/Sao_Paulo");

    private final MailGateway mailGateway;
    private final AccountLinkProperties properties;

    @Async(MailGatewayConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLinkRequested(AccountLinkMailRequestedEvent event) {
        OutgoingMail mail = event.purpose() == AccountTokenPurpose.ACTIVATION ? activationMail(event) : unlockMail(event);
        try {
            mailGateway.send(mail);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail '{}' para {}: {}", mail.subject(), mail.maskedRecipient(), e.getMessage());
        }
    }

    OutgoingMail activationMail(AccountLinkMailRequestedEvent event) {
        String body = """
                Olá, %s!

                Confirme seu e-mail para ativar sua conta:
                %s

                O link vale até %s e só pode ser usado uma vez.
                Se você não criou esta conta, ignore esta mensagem.
                """.formatted(event.recipientName(), link("/auth/verify-email", event.rawToken()), format(event));
        return new OutgoingMail(event.recipientEmail(), "Confirme seu e-mail", body);
    }

    OutgoingMail unlockMail(AccountLinkMailRequestedEvent event) {
        String body = """
                Olá, %s!

                Sua conta foi bloqueada após várias tentativas de login com senha incorreta.
                Ela será liberada automaticamente em %s, ou agora por este link (uso único):
                %s

                Se não foi você, recomendamos trocar sua senha após o desbloqueio.
                """.formatted(event.recipientName(), format(event), link("/auth/unlock", event.rawToken()));
        return new OutgoingMail(event.recipientEmail(), "Sua conta foi bloqueada", body);
    }

    private String link(String path, String rawToken) {
        return properties.getFrontendBaseUrl() + path + "?token=" + rawToken;
    }

    private static String format(AccountLinkMailRequestedEvent event) {
        return event.validUntil().atZoneSameInstant(BRAZIL).format(TIME);
    }
}
