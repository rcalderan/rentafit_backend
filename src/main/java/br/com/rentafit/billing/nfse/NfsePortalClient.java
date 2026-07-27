package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.dto.DpsResponse;
import br.com.rentafit.billing.service.StsTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Envia o DPS assinado para o Portal Nacional da NFS-e via REST.
 *
 * <p>Exemplo: {@code client.sendDps(signedXml)} → {@code Mono<DpsResponse>}.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NfsePortalClient {

    @Qualifier("nfseWebClient")
    private final WebClient nfseWebClient;
    private final StsTokenService stsTokenService;
    private final NfseDpsPayloadEncoder payloadEncoder;

    public Mono<DpsResponse> sendDps(String signedXml) {
        NfseDpsPayload payload = payloadEncoder.encode(signedXml);
        return stsTokenService.obterToken()
                .flatMap(token -> nfseWebClient.post()
                        .uri("/nfse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers -> headers.setBearerAuth(token))
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(DpsResponse.class))
                .doOnSuccess(response -> log.info("DPS aceita pelo portal: chave={}", response.getAccessKey()))
                .doOnError(error -> log.error("Erro ao enviar DPS ao portal: {}", error.getMessage()));
    }

    public Mono<Boolean> hasNfseForDps(String dpsId) {
        if (dpsId == null || dpsId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Identificador DPS deve conter texto não vazio"));
        }
        return nfseWebClient.head()
                .uri("/dps/{id}", dpsId)
                .exchangeToMono(response -> {
                    if (response.statusCode().value() == 404) {
                        return Mono.just(false);
                    }
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.just(true);
                    }
                    return response.createException().flatMap(Mono::error);
                });
    }
}
