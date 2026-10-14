package br.com.rentafit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Exposes the system clock as a bean so time-based rules (login lockout, link expiry) can be tested with a fixed clock.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
