package br.com.rentafit.people.service;

import br.com.rentafit.common.exception.ExternalServiceTimeoutException;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.dto.ViaCepResponseDTO;
import br.com.rentafit.people.util.ZipCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Service for integrating with ViaCEP external API to fetch address information by ZIP code.
 *
 * ViaCEP (Via Correios) is a free Brazilian postal code lookup service.
 * This service handles:
 * - ZIP code normalization and validation
 * - API communication with retry logic and backoff
 * - Response caching (7 days) to avoid unnecessary external API calls
 * - Error handling with graceful degradation
 *
 * @see <a href="https://viacep.com.br">ViaCEP - API de CEP</a>
 */
@Service
@Slf4j
public class ViaCepIntegrationService {

    private static final String VIA_CEP_BASE_URL = "https://viacep.com.br/ws";
    private final WebClient webClient;

    public ViaCepIntegrationService(@Qualifier("viaCepWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetch address information from ViaCEP API by ZIP code.
     *
     * Results are cached for 7 days to avoid unnecessary external API calls.
     * If the API returns an error or the service is unavailable, null is returned
     * and the caller should handle fallback logic.
     *
     * Retry Logic:
     * - Retries up to 3 times with exponential backoff (1s, 2s, 4s)
     * - Max backoff of 5 seconds
     * - Does NOT retry on 404 (ZIP code not found)
     *
     * @param zipCode ZIP code (with or without hyphen, will be normalized to 8 digits)
     * @return ViaCepResponseDTO with address data, or null if not found/error occurs
     * @throws IllegalArgumentException if zipCode format is invalid
     */
    @Cacheable(value = "viaCepCache", key = "#zipCode", unless = "#result == null || #result.hasError()")
    public ViaCepResponseDTO fetchAddressByZipCode(String zipCode) {
        String normalizedZipCode = ZipCodeUtils.normalize(zipCode);

        // Return null if ZIP code is invalid or null
        if (normalizedZipCode == null) {
            return null;
        }

        String url = String.format("%s/%s/json/", VIA_CEP_BASE_URL, normalizedZipCode);

        log.debug("Fetching address from ViaCEP for ZIP code: {}", normalizedZipCode);

        try {
            // Note: Using .block() here because the calling AddressService is synchronous (running
            // in a standard Spring MVC thread) and requires the result to proceed with JPA operations.
            ViaCepResponseDTO response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ViaCepResponseDTO.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5))
                            .filter(throwable -> !(throwable instanceof WebClientResponseException.NotFound)))
                    .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                        log.warn("ZIP code not found in ViaCEP: {}", normalizedZipCode);
                        throw new ResourceNotFoundException("ViaCep","ZipCode", normalizedZipCode);
                    })
                    .onErrorResume(e -> {
                        Throwable actualError = Exceptions.isRetryExhausted(e) ? e.getCause() : e;

                        log.error("Error fetching address from ViaCEP for ZIP code {}: {}",
                                normalizedZipCode, actualError.getMessage());

                        throw new ExternalServiceTimeoutException(actualError.getMessage(), 5000);
                    })
                    .block();

            if (response == null || response.hasError()) {
                log.warn("ViaCEP returned error or null for ZIP code: {}", normalizedZipCode);
                throw new ResourceNotFoundException("ViaCep","ZipCode", normalizedZipCode);
            }

            log.info("Successfully fetched address from ViaCEP: {} - {}, {}",
                    normalizedZipCode, response.logradouro(), response.localidade());

            return response;
        } catch (ExternalServiceTimeoutException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling ViaCEP for ZIP code {}: {}", normalizedZipCode, e.getMessage());
            throw e;
        }
    }
}
