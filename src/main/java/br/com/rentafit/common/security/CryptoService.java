package br.com.rentafit.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;

@Service
@Slf4j
public class CryptoService {

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
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating RSA keys", e);
        }
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error("Failed to decrypt data: {}", e.getMessage());
            throw new RuntimeException("Invalid encrypted data", e);
        }
    }
}

