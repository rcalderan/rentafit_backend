package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.dto.DpsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Envia o XML DPS assinado para o portal e retorna a resposta.
     *
     * @param signedXml XML DPS já assinado
     * @return Mono com dados da nota emitida
     */
    public Mono<DpsResponse> sendDps(String signedXml) {
        log.debug("Enviando DPS para o Portal Nacional NFS-e");
        return nfseWebClient.post()
                .uri("/v1/dps")
                .header("Content-Type", "application/xml;charset=UTF-8")
                .bodyValue(signedXml)
                .retrieve()
                .bodyToMono(DpsResponse.class)
                .doOnSuccess(r -> log.info("DPS aceito pelo portal: chave={}", r.getAccessKey()))
                .doOnError(e -> log.error("Erro ao enviar DPS ao portal: {}", e.getMessage()));
    }
}
