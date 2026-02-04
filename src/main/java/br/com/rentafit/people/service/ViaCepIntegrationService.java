package br.com.rentafit.people.service;

import br.com.rentafit.common.exception.ExternalServiceTimeoutException;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.people.dto.BrasilApiResponseDTO;
import br.com.rentafit.people.dto.ViaCepResponseDTO;
import br.com.rentafit.people.util.ZipCodeUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
 * - Circuit Breaker pattern to protect against unstable ViaCEP service
 * - Automatic fallback to BrasilAPI when circuit is open
 * - Response caching (7 days) to avoid unnecessary external API calls
 * - Error handling with graceful degradation
 *
 * Circuit Breaker Configuration:
 * - Monitors last 10 calls with minimum 5 calls to activate
 * - Opens circuit when 50% failure rate or 50% slow calls (>3s)
 * - Stays open for 60 seconds before attempting half-open state
 * - Automatically falls back to BrasilAPI when circuit is open
 *
 * @see <a href="https://viacep.com.br">ViaCEP - API de CEP</a>
 * @see <a href="https://brasilapi.com.br">BrasilAPI - Fallback Provider</a>
 */
@Service
@Slf4j
public class ViaCepIntegrationService {

    private static final String VIA_CEP_BASE_URL = "https://viacep.com.br/ws";
    private final WebClient viaCepWebClient;
    private final WebClient brasilApiWebClient;

    public ViaCepIntegrationService(
            @Qualifier("viaCepWebClient") WebClient viaCepWebClient,
            @Qualifier("brasilApiWebClient") WebClient brasilApiWebClient) {
        this.viaCepWebClient = viaCepWebClient;
        this.brasilApiWebClient = brasilApiWebClient;
    }

    /**
     * Fetch address information from ViaCEP API by ZIP code with Circuit Breaker protection.
     *
     * Results are cached for 7 days to avoid unnecessary external API calls.
     * If the API returns an error or the service is unavailable, automatically
     * falls back to BrasilAPI through the Circuit Breaker mechanism.
     *
     * Retry Logic (ViaCEP):
     * - Retries up to 3 times with exponential backoff (1s, 2s, 4s)
     * - Max backoff of 5 seconds
     * - Does NOT retry on 404 (ZIP code not found)
     *
     * Circuit Breaker:
     * - Opens after 50% failure rate in last 10 calls (minimum 5 calls)
     * - Opens after 50% slow calls (>3s)
     * - Stays open for 60 seconds
     * - Automatically redirects to fallbackFetchFromBrasilApi when open
     *
     * @param zipCode ZIP code (with or without hyphen, will be normalized to 8 digits)
     * @return ViaCepResponseDTO with address data, or null if not found/error occurs
     * @throws ResourceNotFoundException if ZIP code not found in both providers
     * @throws ExternalServiceTimeoutException if both providers timeout
     */
    @CircuitBreaker(name = "viacep", fallbackMethod = "fallbackFetchFromBrasilApi")
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
            ViaCepResponseDTO response = viaCepWebClient.get()
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

    /**
     * Fallback method for Circuit Breaker when ViaCEP is unavailable or circuit is open.
     * Automatically tries BrasilAPI as alternative provider.
     *
     * This method is invoked by Resilience4j when:
     * - Circuit is OPEN (too many failures)
     * - Circuit is HALF_OPEN and call fails
     * - Call times out or throws exception
     *
     * @param zipCode ZIP code to fetch
     * @param throwable Exception that triggered the fallback
     * @return ViaCepResponseDTO converted from BrasilAPI, or null if BrasilAPI also fails
     */
    private ViaCepResponseDTO fallbackFetchFromBrasilApi(String zipCode, Throwable throwable) {
        String normalizedZipCode = ZipCodeUtils.normalize(zipCode);

        log.warn("ViaCEP Circuit Breaker activated. Falling back to BrasilAPI for ZIP code: {}. Reason: {}",
                normalizedZipCode, throwable.getMessage());

        if (normalizedZipCode == null) {
            log.error("Invalid ZIP code for fallback: {}", zipCode);
            return null;
        }

        try {
            String url = String.format("/cep/v1/%s", normalizedZipCode);

            log.debug("Fetching address from BrasilAPI (fallback) for ZIP code: {}", normalizedZipCode);

            BrasilApiResponseDTO brasilApiResponse = brasilApiWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(BrasilApiResponseDTO.class)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(3))
                            .filter(ex -> !(ex instanceof WebClientResponseException.NotFound)))
                    .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                        log.warn("ZIP code not found in BrasilAPI (fallback): {}", normalizedZipCode);
                        throw new ResourceNotFoundException("BrasilAPI", "ZipCode", normalizedZipCode);
                    })
                    .onErrorResume(e -> {
                        log.error("Error fetching from BrasilAPI (fallback) for ZIP code {}: {}",
                                normalizedZipCode, e.getMessage());
                        return Mono.empty();
                    })
                    .block(Duration.ofSeconds(5));

            if (brasilApiResponse == null) {
                log.error("BrasilAPI fallback returned null for ZIP code: {}", normalizedZipCode);
                return null;
            }

            ViaCepResponseDTO convertedResponse = brasilApiResponse.toViaCepFormat();

            log.info("Successfully fetched address from BrasilAPI (fallback): {} - {}, {}",
                    normalizedZipCode, convertedResponse.logradouro(), convertedResponse.localidade());

            return convertedResponse;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Fallback to BrasilAPI failed for ZIP code {}: {}", normalizedZipCode, e.getMessage());
            return null;
        }
    }
}
