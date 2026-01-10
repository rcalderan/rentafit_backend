package br.com.rentafit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient configuration for ViaCEP external API calls
 */
@Configuration
public class ViaCepWebClientConfig {

    @Bean
    public WebClient viaCepWebClient(WebClient.Builder builder) {
        return builder
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024)) // 1MB buffer
                .build();
    }
}

