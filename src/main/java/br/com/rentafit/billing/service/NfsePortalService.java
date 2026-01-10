package br.com.rentafit.billing.service;

import br.com.rentafit.billing.dto.DpsRequest;
import br.com.rentafit.billing.dto.DpsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class NfsePortalService {

    private final WebClient nfseWebClient;
    private final StsTokenService stsTokenService;

    public NfsePortalService(@Qualifier("nfseWebClient") WebClient nfseWebClient,
                             StsTokenService stsTokenService) {
        this.nfseWebClient = nfseWebClient;
        this.stsTokenService = stsTokenService;
    }

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

