package br.com.rentafit.billing.config;

import br.com.rentafit.config.CertificateConfig;
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
import java.security.KeyStore;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SaoCarlosWebClientConfig {

    private final CertificateConfig certificateConfig;

    @Value("${nfs-e.saocarlos.ambiente}")
    private String ambiente;

    @Value("${nfs-e.saocarlos.url.homologacao}")
    private String urlHomologacao;

    @Value("${nfs-e.saocarlos.url.producao}")
    private String urlProducao;

    @Value("${nfs-e.certificate.password}")
    private String certificatePassword;

    /**
     * WebClient para comunicação SOAP com o webservice GINFES de São Carlos.
     * Usa mTLS com certificado ICP-Brasil.
     */
    @Bean
    public WebClient saoCarlosWebClient() {
        String baseUrl = "2".equals(ambiente) ? urlHomologacao : urlProducao;
        return criarWebClientComMtls(baseUrl, "NFS-e São Carlos (GINFES)");
    }

    private WebClient criarWebClientComMtls(String baseUrl, String nomeCliente) {
        try {
            KeyStore keyStore = certificateConfig.nfsKeyStore();
            WebClient.Builder builder = WebClient.builder();

            if (keyStore == null) {
                log.warn("Certificado não configurado. {} funcionará sem mTLS.", nomeCliente);
                return builder.baseUrl(baseUrl).build();
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, certificatePassword.toCharArray());

            SslContext sslContext = SslContextBuilder.forClient()
                    .keyManager(kmf)
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

            log.info("{} configurado com mTLS para: {}", nomeCliente, baseUrl);

            return builder
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .defaultHeader("Content-Type", "text/xml; charset=utf-8")
                    .defaultHeader("SOAPAction", "")
                    .build();

        } catch (Exception e) {
            log.error("Erro ao configurar {} com mTLS: {}", nomeCliente, e.getMessage());
            return WebClient.builder().baseUrl(baseUrl).build();
        }
    }
}
