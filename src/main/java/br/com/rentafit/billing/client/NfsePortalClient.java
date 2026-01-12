package br.com.rentafit.billing.client;

import br.com.rentafit.billing.dto.DpsRequest;
import br.com.rentafit.billing.dto.DpsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class NfsePortalClient {

    private final WebClient nfseWebClient;
    private final StsTokenService stsTokenService;

    /**
     * Envia um DPS para o Portal Nacional.
     * Obtém token OAuth2 automaticamente via STS antes de enviar.
     */
    public Mono<DpsResponse> sendDps(DpsRequest request) {
        return stsTokenService.obterToken()
                .flatMap(token -> enviarDpsComToken(request, token));
    }

    /**
     * Envia DPS com token já obtido.
     */
    private Mono<DpsResponse> enviarDpsComToken(DpsRequest request, String token) {
        return nfseWebClient.post()
                .uri("/dps")
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DpsResponse.class)
                .doOnSuccess(res -> log.info("DPS enviado com sucesso. Protocolo: {}", res.getProtocol()))
                .doOnError(err -> log.error("Erro ao enviar DPS: {}", err.getMessage()));
    }
}
