package br.com.rentafit.billing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * Carrega o truststore dedicado para CAs da SEFAZ/SEFIN (ICP-Brasil).
 *
 * <p>Segue o padrão dos projetos de referência (amaica/nfse2, Samuel-Oliveira/Java_NFe):
 * truststore JKS separado do certificado do cliente, com fallback para o cacerts do JDK.</p>
 *
 * <p>Exemplo de uso:</p>
 * <pre>{@code
 * TrustManagerFactory tmf = truststoreProvider.trustManagerFactory();
 * SslContext sslContext = SslContextBuilder.forClient()
 *     .keyManager(kmf)
 *     .trustManager(tmf)
 *     .build();
 * }</pre>
 */
@Component
@Slf4j
public class SefazTruststoreProvider {

    @Value("${nfs-e.truststore.path:}")
    private String truststorePath;

    @Value("${nfs-e.truststore.password:changeit}")
    private String truststorePassword;

    @Value("${nfs-e.truststore.max-age-days:90}")
    private long maxAgeDays;

    private KeyStore cachedTrustStore;
    private boolean loaded = false;

    /**
     * Retorna o KeyStore de confiança (JKS) ou null se não configurado.
     * Resultado é cacheado após a primeira chamada.
     */
    public KeyStore trustStore() {
        if (!loaded) {
            cachedTrustStore = carregarTrustStore();
            loaded = true;
        }
        return cachedTrustStore;
    }

    /**
     * Retorna o TrustManagerFactory configurado com o truststore dedicado,
     * ou null para usar o default do JDK (cacerts).
     */
    public TrustManagerFactory trustManagerFactory() {
        KeyStore ts = trustStore();
        if (ts == null) {
            log.debug("Truststore dedicado não configurado. Usando cacerts padrão do JDK.");
            return null;
        }
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            log.info("TrustManagerFactory inicializado com truststore dedicado ({} certificados).",
                    contarCertificados(ts));
            return tmf;
        } catch (Exception e) {
            log.error("Erro ao inicializar TrustManagerFactory com truststore dedicado: {}. " +
                    "Fazendo fallback para cacerts do JDK.", e.getMessage());
            return null;
        }
    }

    private KeyStore carregarTrustStore() {
        if (truststorePath == null || truststorePath.isBlank()) {
            log.info("Truststore dedicado não configurado (nfs-e.truststore.path vazio). " +
                    "Será usado o cacerts padrão do JDK.");
            return null;
        }

        File file = new File(truststorePath);

        if (!file.exists()) {
            log.warn("Truststore não encontrado em: {}. Gerando sincronamente...", truststorePath);
            return gerarSincronamente(file);
        }

        KeyStore ks = lerArquivo(file);
        if (ks == null) {
            return null;
        }

        verificarERegenerarEmBackground(file);
        return ks;
    }

    private KeyStore gerarSincronamente(File file) {
        try {
            SefazCacertsGenerator.gerar(truststorePath, truststorePassword);
            return lerArquivo(file);
        } catch (Exception e) {
            log.error("Falha ao gerar truststore em {}: {}. " +
                    "Será usado o cacerts padrão do JDK.", truststorePath, e.getMessage());
            return null;
        }
    }

    private void verificarERegenerarEmBackground(File file) {
        long idadeDias = Duration.between(
                Instant.ofEpochMilli(file.lastModified()),
                Instant.now()
        ).toDays();

        if (idadeDias < maxAgeDays) {
            log.debug("Truststore tem {} dias (máximo: {}). Sem necessidade de renovação.",
                    idadeDias, maxAgeDays);
            return;
        }

        log.info("Truststore tem {} dias (máximo: {}). Iniciando regeneração em background...",
                idadeDias, maxAgeDays);

        CompletableFuture.runAsync(() -> {
            try {
                SefazCacertsGenerator.gerar(truststorePath, truststorePassword);
                log.info("Truststore regenerado em background com sucesso. " +
                        "Novo arquivo será usado na próxima inicialização.");
            } catch (Exception e) {
                log.warn("Regeneração em background do truststore falhou: {}. " +
                        "Arquivo existente continua válido.", e.getMessage());
            }
        });
    }

    private KeyStore lerArquivo(File file) {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(file)) {
                ks.load(fis, truststorePassword.toCharArray());
            }
            log.info("Truststore carregado: {} ({} certificados)", truststorePath, contarCertificados(ks));
            return ks;
        } catch (Exception e) {
            log.error("Falha ao carregar truststore de {}: {}. " +
                    "Será usado o cacerts padrão do JDK.", truststorePath, e.getMessage());
            return null;
        }
    }

    private int contarCertificados(KeyStore ks) {
        try {
            int count = 0;
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.getCertificate(alias) instanceof X509Certificate) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return -1;
        }
    }
}
