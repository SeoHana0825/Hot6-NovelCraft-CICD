package com.example.hot6novelcraft.domain.exchange.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AesEncryptionUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private final SecretKeySpec secretKey;
    private final IvParameterSpec iv;

    public AesEncryptionUtil(
            @Value("${encryption.aes.secret-key}") String key,
            @Value("${encryption.aes.iv}") String ivValue
    ) {
        this.secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        this.iv = new IvParameterSpec(ivValue.getBytes(StandardCharsets.UTF_8));
    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("계좌번호 암호화에 실패했습니다", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("계좌번호 복호화에 실패했습니다", e);
        }
    }
}