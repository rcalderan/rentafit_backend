package br.com.rentafit.billing.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class NfseWebClientConfig {

    private final FiscalCertificateProvider certificateProvider;
    private final SefazTruststoreProvider truststoreProvider;

    @Value("${nfs-e.api.url}")
    private String nfseBaseUrl;

    @Value("${nfs-e.sts.url:https://hom.nfse.gov.br/api/token}")
    private String stsUrl;

    @Value("${nfs-e.certificate.password:}")
    private String certificatePassword;

    @Value("${nf-e.sefaz.url:https://homologacao.nfe.fazenda.sp.gov.br}")
    private String sefazBaseUrl;

    /**
     * WebClient para chamadas à API NFS-e (DPS, consultas, eventos).
     * Usa mTLS com certificado ICP-Brasil.
     */
    @Bean
    public WebClient nfseWebClient() {
        return criarWebClientComMtls(nfseBaseUrl, "API NFS-e");
    }

    /**
     * WebClient dedicado para obter tokens no STS.
     * Também usa mTLS com o mesmo certificado.
     */
    @Bean
    public WebClient stsWebClient() {
        return criarWebClientComMtls(stsUrl, "STS");
    }

    /**
     * WebClient para transmissão de NF-e (modelo 55) à SEFAZ-SP.
     * Usa mTLS com certificado ICP-Brasil A1.
     */
    @Bean
    public WebClient nfeSefazWebClient() {
        return criarWebClientComMtls(sefazBaseUrl, "SEFAZ-SP NF-e");
    }

    /**
     * Cria um WebClient configurado com mTLS usando certificado ICP-Brasil.
     */
    private WebClient criarWebClientComMtls(String baseUrl, String nomeCliente) {
        try {
            KeyStore keyStore = certificateProvider.keyStore();
            WebClient.Builder builder = WebClient.builder();

            if (keyStore == null) {
                log.warn("Certificado não configurado. {} funcionará sem mTLS.", nomeCliente);
                return builder.baseUrl(baseUrl).build();
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, certificatePassword.toCharArray());

            SslContextBuilder sslBuilder = SslContextBuilder.forClient()
                    .keyManager(kmf);

            TrustManagerFactory tmf = truststoreProvider.trustManagerFactory();
            if (tmf != null) {
                sslBuilder.trustManager(tmf);
                log.debug("{} usando truststore dedicado para validação do servidor.", nomeCliente);
            }

            SslContext sslContext = sslBuilder.build();

            HttpClient httpClient = HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

            log.info("{} configurado com mTLS para: {}", nomeCliente, baseUrl);

            return builder.baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();

        } catch (Exception e) {
            log.error("Erro ao configurar {} com mTLS: {}", nomeCliente, e.getMessage());
            return WebClient.builder().baseUrl(baseUrl).build();
        }
    }
}

