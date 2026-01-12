package br.com.rentafit.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Cipher;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoServiceTest {

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService();
        ReflectionTestUtils.setField(cryptoService, "rsaEnabled", true);
        ReflectionTestUtils.invokeMethod(cryptoService, "generateKeyPair");
    }

    @Test
    @DisplayName("Should rotate keys and generate a new public key")
    void rotateKeys() {
        String oldKey = cryptoService.getPublicKeyBase64();
        cryptoService.rotateKeys();
        String newKey = cryptoService.getPublicKeyBase64();

        assertThat(oldKey).isNotEqualTo(newKey);
    }

    @Test
    @DisplayName("Should decrypt data encrypted with public key")
    void decrypt() throws Exception {
        String originalData = "my-secret-password";
        String pemKey = cryptoService.getPublicKeyBase64();

        // Encrypt with public key (simulating frontend)
        // Clean PEM to get raw base64
        String publicKeyBase64 = pemKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(spec);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(originalData.getBytes());
        String encryptedData = Base64.getEncoder().encodeToString(encryptedBytes);

        // Decrypt with service
        String decryptedData = cryptoService.decrypt(encryptedData);

        assertThat(decryptedData).isEqualTo(originalData);
    }

    @Test
    @DisplayName("Should throw exception for invalid encrypted data")
    void decrypt_invalid() {
        assertThatThrownBy(() -> cryptoService.decrypt("not-base64-and-not-encrypted"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid encrypted data");
    }

    @Test
    @DisplayName("Should throw exception when RSA is disabled")
    void rsaDisabled() {
        ReflectionTestUtils.setField(cryptoService, "rsaEnabled", false);
        ReflectionTestUtils.setField(cryptoService, "keyPair", null);

        assertThatThrownBy(() -> cryptoService.getPublicKeyBase64())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RSA encryption is disabled");

        assertThatThrownBy(() -> cryptoService.decrypt("some-data"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RSA encryption is disabled");
    }
}
