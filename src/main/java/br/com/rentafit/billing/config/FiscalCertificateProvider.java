package br.com.rentafit.billing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    private record CertificateEntry(PrivateKey privateKey, X509Certificate certificate) {
    }

    private CertificateEntry loadedEntry;

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
     * Retorna a PrivateKey do certificado final (não-CA) do KeyStore, ou null.
     * Usado para assinar XML com Santuario/XMLDSig.
     */
    public PrivateKey privateKey() {
        return entry() != null ? entry().privateKey() : null;
    }

    /**
     * Retorna o X509Certificate final (não-CA) do KeyStore, ou null.
     * Usado para incluir o certificado na assinatura XML.
     */
    public X509Certificate certificate() {
        return entry() != null ? entry().certificate() : null;
    }

    private CertificateEntry entry() {
        if (loadedEntry == null) {
            loadedEntry = findEndEntityEntry();
        }
        return loadedEntry;
    }

    private CertificateEntry findEndEntityEntry() {
        KeyStore ks = keyStore();
        if (ks == null) return null;

        try {
            char[] pass = certificatePassword.toCharArray();
            var aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (!ks.isKeyEntry(alias)) {
                    continue;
                }
                PrivateKey key = (PrivateKey) ks.getKey(alias, pass);
                if (key == null) {
                    continue;
                }

                Certificate[] chain = ks.getCertificateChain(alias);
                X509Certificate cert = resolveEndEntityCertificate(chain);
                if (cert == null) {
                    cert = resolveEndEntityCertificate(new java.security.cert.Certificate[]{ks.getCertificate(alias)});
                }

                if (cert == null) {
                    log.warn("Alias '{}' possui chave privada, mas nenhum certificado final (CA=false) foi encontrado.", alias);
                    continue;
                }

                if (!hasSigningKeyUsage(cert)) {
                    log.warn("Alias '{}' certificado final não possui KeyUsage DigitalSignature/NonRepudiation.", alias);
                    continue;
                }

                List<String> cnpjs = extractCnpjsFromSan(cert);
                if (cnpjs.isEmpty()) {
                    log.warn("Alias '{}' certificado final não possui CNPJ na extensão SubjectAlternativeName.", alias);
                    continue;
                }

                log.info("Certificado final selecionado para assinatura: alias={}, subject={}, cnpj(s)={}",
                        alias, cert.getSubjectX500Principal(), cnpjs);
                return new CertificateEntry(key, cert);
            }
        } catch (Exception e) {
            log.error("Erro ao selecionar certificado final no PKCS12: {}", e.getMessage());
        }

        log.error("Nenhum certificado final (CA=false, com chave privada e CNPJ na SAN) foi encontrado no PKCS12.");
        return null;
    }

    private X509Certificate resolveEndEntityCertificate(java.security.cert.Certificate[] chain) {
        if (chain == null) {
            return null;
        }
        for (java.security.cert.Certificate c : chain) {
            if (c instanceof X509Certificate cert && isEndEntityCertificate(cert)) {
                return cert;
            }
        }
        return null;
    }

    private boolean isEndEntityCertificate(X509Certificate cert) {
        return cert != null && cert.getBasicConstraints() == -1;
    }

    private boolean hasSigningKeyUsage(X509Certificate cert) {
        boolean[] usage = cert.getKeyUsage();
        if (usage == null) {
            return false;
        }
        // bit 0 = digitalSignature, bit 1 = nonRepudiation (IETF RFC 5280)
        return usage.length > 0 && usage[0] || (usage.length > 1 && usage[1]);
    }

    private List<String> extractCnpjsFromSan(X509Certificate cert) {
        Collection<List<?>> sans;
        try {
            sans = cert.getSubjectAlternativeNames();
        } catch (java.security.cert.CertificateParsingException e) {
            log.warn("Falha ao ler SubjectAlternativeNames do certificado: {}", e.getMessage());
            return List.of();
        }
        if (sans == null) {
            return List.of();
        }
        List<String> cnpjs = new ArrayList<>();
        for (List<?> san : sans) {
            if (san == null || san.size() < 2) {
                continue;
            }
            Object type = san.get(0);
            if (!(type instanceof Integer integerType) || integerType != 0) {
                continue;
            }
            Object value = san.get(1);
            if (value instanceof byte[] der) {
                String cnpj = parseOtherNameCnpj(der);
                if (cnpj != null && !cnpj.isBlank()) {
                    cnpjs.add(cnpj);
                }
            }
        }
        return cnpjs;
    }

    private String parseOtherNameCnpj(byte[] der) {
        if (der == null || der.length < 5) {
            return null;
        }
        for (int i = 0; i <= der.length - 5; i++) {
            if (isIcpBrasilCnpjOid(der, i)) {
                String cnpj = findFourteenConsecutiveDigits(der, Math.max(0, i - 10), Math.min(der.length, i + 30));
                if (cnpj != null) {
                    return cnpj;
                }
            }
        }
        return null;
    }

    private boolean isIcpBrasilCnpjOid(byte[] der, int i) {
        return der[i] == 0x60
                && der[i + 1] == 0x4C
                && der[i + 2] == 0x01
                && der[i + 3] == 0x03
                && (der[i + 4] == 0x03 || der[i + 4] == 0x04);
    }

    private String findFourteenConsecutiveDigits(byte[] data, int start, int end) {
        int count = 0;
        int startIdx = -1;
        for (int i = start; i < end; i++) {
            byte b = data[i];
            if (b >= '0' && b <= '9') {
                if (startIdx == -1) {
                    startIdx = i;
                }
                count++;
                if (count == 14) {
                    return new String(data, startIdx, 14, java.nio.charset.StandardCharsets.UTF_8);
                }
            } else {
                startIdx = -1;
                count = 0;
            }
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
