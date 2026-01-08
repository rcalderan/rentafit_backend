package br.com.rentafit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.security.KeyStore;

@Configuration
@Slf4j
public class CertificateConfig {

    @Value("${nfs-e.certificate.path:}")
    private String certificatePath;

    @Value("${nfs-e.certificate.password:}")
    private String certificatePassword;

    @Bean
    public KeyStore nfsKeyStore() {
        if (certificatePath == null || certificatePath.isEmpty()) {
            log.warn("Certificado digital não configurado. Funcionalidades de NFS-e estarão limitadas.");
            return null;
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream is = new FileInputStream(certificatePath)) {
                keyStore.load(is, certificatePassword.toCharArray());
            }
            log.info("Certificado digital carregado com sucesso a partir de: {}", certificatePath);
            return keyStore;
        } catch (Exception e) {
            log.error("Erro ao carregar o certificado digital: {}", e.getMessage());
            return null;
        }
    }
}
