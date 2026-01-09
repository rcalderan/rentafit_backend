package br.com.rentafit.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    @Value("${rentafit.security.db.encryption-key:defaultSecretKey123!}")
    private String secretKey;

    public String encrypt(String strToEncrypt) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            SecretKeySpec keySpec = new SecretKeySpec(getFixedKey(), "AES");

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
            byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes());

            byte[] cipherTextWithIv = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, cipherTextWithIv, 0, iv.length);
            System.arraycopy(cipherText, 0, cipherTextWithIv, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(cipherTextWithIv);
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting", e);
        }
    }

    public String decrypt(String strToDecrypt) {
        try {
            byte[] decode = Base64.getDecoder().decode(strToDecrypt);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(decode, 0, iv, 0, iv.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            SecretKeySpec keySpec = new SecretKeySpec(getFixedKey(), "AES");

            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
            byte[] plainText = cipher.doFinal(decode, IV_LENGTH_BYTE, decode.length - IV_LENGTH_BYTE);

            return new String(plainText);
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting", e);
        }
    }

    private byte[] getFixedKey() {
        // Garantir que a chave tenha 32 bytes para AES-256
        byte[] keyBytes = new byte[32];
        byte[] secretBytes = secretKey.getBytes();
        System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, 32));
        return keyBytes;
    }
}

