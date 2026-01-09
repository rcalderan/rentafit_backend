package br.com.rentafit.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesCryptoServiceTest {

    private AesCryptoService aesCryptoService;

    @BeforeEach
    void setUp() {
        aesCryptoService = new AesCryptoService();
        ReflectionTestUtils.setField(aesCryptoService, "secretKey", "my-very-long-and-secure-secret-key-123!");
    }

    @Test
    @DisplayName("Should encrypt and decrypt successfully")
    void encryptDecrypt() {
        String original = "sensitive-data-123";
        String encrypted = aesCryptoService.encrypt(original);

        assertThat(encrypted).isNotEqualTo(original);

        String decrypted = aesCryptoService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("Should produce different results for same input due to IV")
    void encrypt_randomness() {
        String data = "data";
        String encrypted1 = aesCryptoService.encrypt(data);
        String encrypted2 = aesCryptoService.encrypt(data);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(aesCryptoService.decrypt(encrypted1)).isEqualTo(data);
        assertThat(aesCryptoService.decrypt(encrypted2)).isEqualTo(data);
    }

    @Test
    @DisplayName("Should throw exception for malformed data")
    void decrypt_invalid() {
        assertThatThrownBy(() -> aesCryptoService.decrypt("invalid-data"))
                .isInstanceOf(RuntimeException.class);
    }
}

