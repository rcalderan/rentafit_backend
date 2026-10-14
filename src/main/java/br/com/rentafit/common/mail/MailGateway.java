package br.com.rentafit.common.mail;

/**
 * Project-owned port for outgoing e-mail, so business code does not depend on JavaMail directly.
 *
 * <p>Usage: {@code mailGateway.send(new OutgoingMail("ana@ex.com", "Assunto", "Corpo"))}.</p>
 */
public interface MailGateway {

    void send(OutgoingMail mail);
}
