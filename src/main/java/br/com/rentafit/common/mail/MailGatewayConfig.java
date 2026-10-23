package br.com.rentafit.common.mail;

import br.com.rentafit.auth.config.AccountLinkProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Picks the SMTP gateway when Spring Boot configured a JavaMailSender (spring.mail.host set), otherwise a no-op.
 * Mail runs on its own executor so a long migration on the shared executor never delays activation e-mails.
 */
@Configuration
public class MailGatewayConfig {

    public static final String MAIL_EXECUTOR = "accountMailExecutor";

    @Bean
    public MailGateway mailGateway(ObjectProvider<JavaMailSender> sender, AccountLinkProperties properties,
                                   @Value("${spring.mail.host:}") String smtpHost) {
        JavaMailSender available = sender.getIfAvailable();
        // Compose files pass SPRING_MAIL_HOST="" when unset, which still creates a JavaMailSender with no host.
        if (available == null || smtpHost.isBlank()) {
            return new DisabledMailGateway();
        }
        return new SmtpMailGateway(available, properties.getMailFrom());
    }

    @Bean(name = MAIL_EXECUTOR)
    public TaskExecutor accountMailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("account-mail-");
        executor.initialize();
        return executor;
    }
}
