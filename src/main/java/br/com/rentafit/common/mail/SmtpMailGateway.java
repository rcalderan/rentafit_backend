package br.com.rentafit.common.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Sends e-mail through the SMTP server configured in {@code spring.mail.*} (Mailpit locally).
 */
@RequiredArgsConstructor
@Slf4j
public class SmtpMailGateway implements MailGateway {

    private final JavaMailSender sender;
    private final String from;

    @Override
    public void send(OutgoingMail mail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(mail.to());
        message.setSubject(mail.subject());
        message.setText(mail.body());
        sender.send(message);
        log.info("E-mail '{}' enviado para {}", mail.subject(), mail.maskedRecipient());
    }
}
