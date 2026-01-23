package br.com.rentafit.billing.service;

import br.com.rentafit.billing.dto.DpsRequest;
import br.com.rentafit.billing.dto.DpsResponse;
import br.com.rentafit.billing.dto.NfseConsultaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
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
     * Consulta os detalhes de uma NFS-e autorizada pela chave de acesso
     */
    public Mono<NfseConsultaResponse> consultarNfse(String chaveAcesso) {
        log.info("Consultando NFS-e com chave: {}", chaveAcesso);
        return stsTokenService.obterToken()
                .flatMap(token -> nfseWebClient.get()
                        .uri("/nfse/{chave}", chaveAcesso)
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(NfseConsultaResponse.class)
                        .doOnSuccess(res -> log.info("NFS-e consultada: {} - Status: {}",
                                chaveAcesso, res.getStatus()))
                        .doOnError(err -> log.error("Erro ao consultar NFS-e {}: {}",
                                chaveAcesso, err.getMessage())));
    }

    /**
     * Baixa o PDF (DANFSe) de uma NFS-e autorizada
     */
    public Mono<byte[]> downloadPdf(String chaveAcesso) {
        log.info("Baixando PDF da NFS-e: {}", chaveAcesso);
        return stsTokenService.obterToken()
                .flatMap(token -> nfseWebClient.get()
                        .uri("/nfse/pdf/{chave}", chaveAcesso)
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .bodyToFlux(DataBuffer.class)
                        .map(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            return bytes;
                        })
                        .reduce(new byte[0], this::concatenateBytes)
                        .doOnSuccess(bytes -> log.info("PDF baixado: {} bytes", bytes.length))
                        .doOnError(err -> log.error("Erro ao baixar PDF da NFS-e {}: {}",
                                chaveAcesso, err.getMessage())));
    }

    /**
     * Baixa o XML legal de uma NFS-e autorizada
     */
    public Mono<String> downloadXml(String chaveAcesso) {
        log.info("Baixando XML da NFS-e: {}", chaveAcesso);
        return stsTokenService.obterToken()
                .flatMap(token -> nfseWebClient.get()
                        .uri("/nfse/xml/{chave}", chaveAcesso)
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnSuccess(xml -> log.info("XML baixado: {} caracteres", xml.length()))
                        .doOnError(err -> log.error("Erro ao baixar XML da NFS-e {}: {}",
                                chaveAcesso, err.getMessage())));
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

    /**
     * Concatena arrays de bytes para construir o PDF completo
     */
    private byte[] concatenateBytes(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}

