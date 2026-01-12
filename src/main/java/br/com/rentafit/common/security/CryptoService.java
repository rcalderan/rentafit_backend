package br.com.rentafit.common.security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;

@Service
@Slf4j
public class CryptoService {

    @Getter
    @Value("${app.security.rsa.enabled:false}")
    private boolean rsaEnabled;

    private KeyPair keyPair;

    public CryptoService() {
        generateKeyPair();
    }

    // Rotaciona a chave a cada 24 horas (exemplo)
    @Scheduled(fixedRate = 86400000)
    public void rotateKeys() {
        log.info("Rotating RSA KeyPair...");
        generateKeyPair();
    }

    private void generateKeyPair() {
        if (!rsaEnabled) {
            log.info("RSA encryption is disabled. Skipping key pair generation.");
            return;
        }
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating RSA keys", e);
        }
    }

    public String getPublicKeyBase64() {
        if (!rsaEnabled || keyPair == null) {
            throw new IllegalStateException("RSA encryption is disabled");
        }
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        String base64Key = Base64.getEncoder().encodeToString(publicKeyBytes);
        
        // Formata a chave no formato PEM para compatibilidade com JSEncrypt
        StringBuilder pemKey = new StringBuilder();
        pemKey.append("-----BEGIN PUBLIC KEY-----\n");
        
        // Adiciona quebras de linha a cada 64 caracteres (padrão PEM)
        int index = 0;
        while (index < base64Key.length()) {
            pemKey.append(base64Key, index, Math.min(index + 64, base64Key.length()));
            pemKey.append("\n");
            index += 64;
        }
        
        pemKey.append("-----END PUBLIC KEY-----");
        return pemKey.toString();
    }

    public String decrypt(String encryptedData) {
        if (!rsaEnabled || keyPair == null) {
            throw new IllegalStateException("RSA encryption is disabled");
        }
        try {
            // Usa PKCS1Padding para compatibilidade com JSEncrypt
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error("Failed to decrypt data: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid encrypted data", e);
        }
    }
}
