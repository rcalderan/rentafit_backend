package br.com.rentafit.billing.client;
import br.com.rentafit.billing.dto.StsTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
@Service
@Slf4j
public class StsTokenService {
    private static final long RENEWAL_MARGIN_SECONDS = 60;
    @Value("${nfs-e.sts.enabled:true}")
    private boolean stsEnabled;
    private final WebClient stsWebClient;
    private final AtomicReference<TokenCache> tokenCache = new AtomicReference<>();
    public StsTokenService(WebClient stsWebClient) {
        this.stsWebClient = stsWebClient;
    }
    public Mono<String> obterToken() {
        if (!stsEnabled) {
            log.warn("STS desabilitado. Retornando token mock.");
            return Mono.just("mock-token-for-testing");
        }
        TokenCache cache = tokenCache.get();
        if (cache != null && cache.isValido()) {
            log.debug("Usando token em cache. Expira: {}", cache.expiration);
            return Mono.just(cache.token);
        }
        return renovarToken();
    }
    private synchronized Mono<String> renovarToken() {
        TokenCache cache = tokenCache.get();
        if (cache != null && cache.isValido()) {
            return Mono.just(cache.token);
        }
        log.info("Renovando token OAuth2 via STS");
        return stsWebClient.post()
                .uri("")
                .retrieve()
                .bodyToMono(StsTokenResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(signal -> 
                                log.warn("Tentativa {} de obter token", signal.totalRetries() + 1)))
                .doOnNext(response -> {
                    Instant expiration = Instant.now()
                            .plusSeconds(response.getExpiresIn() - RENEWAL_MARGIN_SECONDS);
                    tokenCache.set(new TokenCache(response.getAccessToken(), expiration));
                    log.info("Token obtido. Válido até: {}", expiration);
                })
                .map(StsTokenResponse::getAccessToken)
                .doOnError(error -> {
                    log.error("Falha ao obter token: {}", error.getMessage());
                    tokenCache.set(null);
                });
    }
    public void invalidarCache() {
        tokenCache.set(null);
        log.info("Cache de token invalidado");
    }
    public boolean hasTokenValido() {
        TokenCache cache = tokenCache.get();
        return cache != null && cache.isValido();
    }
    private static class TokenCache {
        private final String token;
        private final Instant expiration;
        public TokenCache(String token, Instant expiration) {
            this.token = token;
            this.expiration = expiration;
        }
        public boolean isValido() {
            return Instant.now().isBefore(expiration);
        }
    }
}