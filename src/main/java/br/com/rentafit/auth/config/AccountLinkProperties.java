package br.com.rentafit.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Settings for the single-use links e-mailed to users (account activation and unlock).
 */
@Data
@Component
@ConfigurationProperties(prefix = "rentafit.account-links")
public class AccountLinkProperties {

    /** Public URL of the Angular app, used to build the links (no trailing slash). */
    private String frontendBaseUrl = "http://localhost";

    private Duration activationTtl = Duration.ofHours(24);

    private Duration unlockTtl = Duration.ofMinutes(30);

    private String mailFrom = "nao-responda@rentafit.local";
}
