package br.com.rentafit.billing.nfse;

import br.com.rentafit.billing.dto.DpsResponse;
import br.com.rentafit.billing.dto.NfseConsultaResponse;
import br.com.rentafit.billing.service.StsTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente unificado para o Portal Nacional da NFS-e (SEFIN/ADN).
 *
 * <p>Endpoints seguem o padrão oficial: {@code /sefinnacional/nfse}.</p>
 * <p>O DPS é transmitido como JSON {@code {"dpsXmlGZipB64": "..."}} após GZip + Base64.</p>
 *
 * <p>Exemplo: {@code client.sendDps(signedXml)} → {@code Mono<DpsResponse>}.</p>
 */
@Component
@Slf4j
public class NfsePortalClient {

    private static final String NFSE_PATH = "/sefinnacional/nfse";

    private final WebClient nfseWebClient;
    private final StsTokenService stsTokenService;
    private final NfseDpsPayloadEncoder payloadEncoder;

    public NfsePortalClient(
            @Qualifier("nfseWebClient") WebClient nfseWebClient,
            StsTokenService stsTokenService,
            NfseDpsPayloadEncoder payloadEncoder
    ) {
        this.nfseWebClient = nfseWebClient;
        this.stsTokenService = stsTokenService;
        this.payloadEncoder = payloadEncoder;
    }

    /**
     * Envia o DPS assinado (GZip+Base64) ao Portal Nacional.
     *
     * @param signedXml XML do DPS já assinado
     * @return resposta com chave de acesso e protocolo
     */
    public Mono<DpsResponse> sendDps(String signedXml) {
        NfseDpsPayload payload = payloadEncoder.encode(signedXml);
        return stsTokenService.obterToken()
                .flatMap(token -> nfseWebClient.post()
                        .uri(NFSE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers -> headers.setBearerAuth(token))
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(DpsResponse.class))
                .doOnSuccess(response -> log.info("DPS aceita pelo portal: chave={}", response.getAccessKey()))
                .doOnError(error -> log.error("Erro ao enviar DPS ao portal: {}", error.getMessage()));
    }

    /**
     * Verifica idempotência: checa se já existe NFS-e para o DPS informado.
     * Usa HEAD /sefinnacional/dps/{id} conforme padrão oficial.
     *
     * @param dpsId identificador do DPS
     * @return true se já foi emitida, false se não
     */
    public Mono<Boolean> hasNfseForDps(String dpsId) {
        if (dpsId == null || dpsId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Identificador DPS deve conter texto não vazio"));
        }
        return stsTokenService.obterToken()
                .flatMap(token -> nfseWebClient.head()
                        .uri("/sefinnacional/dps/{id}", dpsId)
                        .headers(h -> h.setBearerAuth(token))
                        .exchangeToMono(response -> {
                            if (response.statusCode().value() == 404) {
                                return Mono.just(false);
                            }
                            if (response.statusCode().is2xxSuccessful()) {
                                return Mono.just(true);
                            }
                            return response.createException().flatMap(Mono::error);
                        }));
    }

    /**
     * Consulta detalhes de uma NFS-e autorizada pela chave de acesso.
     * Endpoint oficial: GET /sefinnacional/nfse/{chave}
     *
     * @param chaveAcesso chave de acesso (50 dígitos)
     * @return detalhes da NFS-e
     */
    public Mono<NfseConsultaResponse> consultarNfse(String chaveAcesso) {
        log.info("Consultando NFS-e com chave: {}", chaveAcesso);
        return stsTokenService.obterToken()
                .flatMap(token -> nfseWebClient.get()
                        .uri(NFSE_PATH + "/{chave}", chaveAcesso)
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(NfseConsultaResponse.class))
                .doOnSuccess(res -> log.info("NFS-e consultada: {} - Status: {}",
                        chaveAcesso, res.getStatus()))
                .doOnError(err -> log.error("Erro ao consultar NFS-e {}: {}",
                        chaveAcesso, err.getMessage()));
    }

    /**
     * Baixa o PDF (DANFSe) de uma NFS-e autorizada.
     * Endpoint oficial: GET /sefinnacional/nfse/{chave}/pdf
     *
     * @param chaveAcesso chave de acesso da NFS-e
     * @return bytes do PDF
     */
    public Mono<byte[]> downloadPdf(String chaveAcesso) {
        log.info("Baixando PDF da NFS-e: {}", chaveAcesso);
        return stsTokenService.obterToken()
                .flatMap(token -> nfseWebClient.get()
                        .uri(NFSE_PATH + "/{chave}/pdf", chaveAcesso)
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(byte[].class))
                .doOnSuccess(bytes -> log.info("PDF baixado: {} bytes", bytes.length))
                .doOnError(err -> log.error("Erro ao baixar PDF da NFS-e {}: {}",
                        chaveAcesso, err.getMessage()));
    }

    /**
     * Baixa o XML legal de uma NFS-e autorizada.
     * Endpoint oficial: GET /sefinnacional/nfse/{chave}/xml
     *
     * @param chaveAcesso chave de acesso da NFS-e
     * @return XML como string
     */
    public Mono<String> downloadXml(String chaveAcesso) {
        log.info("Baixando XML da NFS-e: {}", chaveAcesso);
        return stsTokenService.obterToken()
                .flatMap(token -> nfseWebClient.get()
                        .uri(NFSE_PATH + "/{chave}/xml", chaveAcesso)
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(String.class))
                .doOnSuccess(xml -> log.info("XML baixado: {} caracteres", xml.length()))
                .doOnError(err -> log.error("Erro ao baixar XML da NFS-e {}: {}",
                        chaveAcesso, err.getMessage()));
    }
}
