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
public class NfseViaWebClientConfig {

    private final CertificateConfig certificateConfig;

    @Value("${nfs-e.via.recepcao.url}")
    private String nfseViaRecepcaoUrl;

    @Value("${nfs-e.via.consultas.url}")
    private String nfseViaConsultasUrl;

    @Value("${nfs-e.certificate.password}")
    private String certificatePassword;

    @Bean
    public WebClient nfseViaRecepcaoWebClient() {
        return criarWebClientComMtls(nfseViaRecepcaoUrl, "NFS-e Via Recepção");
    }

    @Bean
    public WebClient nfseViaConsultasWebClient() {
        return criarWebClientComMtls(nfseViaConsultasUrl, "NFS-e Via Consultas");
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

            return builder.baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();

        } catch (Exception e) {
            log.error("Erro ao configurar {} com mTLS: {}", nomeCliente, e.getMessage());
            return WebClient.builder().baseUrl(baseUrl).build();
        }
    }
}
