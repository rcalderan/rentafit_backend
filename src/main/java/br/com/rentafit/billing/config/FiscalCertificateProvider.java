package br.com.rentafit.billing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Carrega o certificado digital A1 (PKCS12) e expõe KeyStore, PrivateKey e X509Certificate
 * para uso em assinatura XML (NFS-e, NF-e) e mTLS (SVRS).
 *
 * <p>Todos os métodos retornam null graciosamente quando o certificado não está configurado
 * ou não pôde ser carregado, permitindo que a aplicação suba sem certificado (homologação mock).</p>
 */
@Configuration
@Slf4j
public class FiscalCertificateProvider {

    @Value("${nfs-e.certificate.path:}")
    private String certificatePath;

    @Value("${nfs-e.certificate.password:}")
    private String certificatePassword;

    private KeyStore loadedKeyStore;
    private boolean loaded = false;

    /**
     * Retorna o KeyStore PKCS12 carregado, ou null se não configurado/indisponível.
     * Resultado é cacheado após a primeira chamada.
     */
    public KeyStore keyStore() {
        if (!loaded) {
            loadedKeyStore = carregarKeyStore();
            loaded = true;
        }
        return loadedKeyStore;
    }

    /**
     * Retorna a primeira PrivateKey encontrada no KeyStore, ou null.
     * Usado para assinar XML com Santuario/XMLDSig.
     */
    public PrivateKey privateKey() {
        KeyStore ks = keyStore();
        if (ks == null) return null;
        try {
            char[] pass = certificatePassword.toCharArray();
            var aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    return (PrivateKey) ks.getKey(alias, pass);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao extrair PrivateKey do certificado: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Retorna o primeiro X509Certificate encontrado no KeyStore, ou null.
     * Usado para incluir o certificado na assinatura XML.
     */
    public X509Certificate certificate() {
        KeyStore ks = keyStore();
        if (ks == null) return null;
        try {
            var aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    return (X509Certificate) ks.getCertificate(alias);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao extrair X509Certificate do certificado: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Retorna true se o KeyStore foi carregado com sucesso.
     * Conveniente para verificações de pré-condição nos serviços de emissão.
     */
    public boolean isAvailable() {
        return keyStore() != null;
    }

    private KeyStore carregarKeyStore() {
        if (certificatePath == null || certificatePath.isBlank()) {
            log.warn("Certificado digital não configurado (nfs-e.certificate.path). Emissão de NF-e/NFS-e estará limitada.");
            return null;
        }

        File file = new File(certificatePath);
        if (!file.exists()) {
            log.warn("Arquivo de certificado não encontrado em: {}. Emissão de NF-e/NFS-e estará limitada.", certificatePath);
            return null;
        }

        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(file)) {
                ks.load(fis, certificatePassword.toCharArray());
            }
            log.info("Certificado digital carregado: {}", certificatePath);
            return ks;
        } catch (Exception e) {
            log.error("Falha ao carregar certificado de {}: {}", certificatePath, e.getMessage());
            return null;
        }
    }
}
