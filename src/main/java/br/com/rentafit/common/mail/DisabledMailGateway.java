package br.com.rentafit.common.mail;

import lombok.extern.slf4j.Slf4j;

/**
 * Fallback when {@code spring.mail.host} is not configured: drops the e-mail and warns.
 * The body is never logged because it contains single-use links.
 */
@Slf4j
public class DisabledMailGateway implements MailGateway {

    @Override
    public void send(OutgoingMail mail) {
        log.warn("SMTP não configurado (spring.mail.host vazio); e-mail '{}' para {} descartado",
                mail.subject(), mail.maskedRecipient());
    }
}
