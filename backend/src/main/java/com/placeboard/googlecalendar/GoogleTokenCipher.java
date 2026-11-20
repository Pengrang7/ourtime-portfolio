package com.placeboard.googlecalendar;

import com.placeboard.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class GoogleTokenCipher {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final byte[] key;
    private final SecureRandom secureRandom = new SecureRandom();

    public GoogleTokenCipher(@Value("${placeboard.google-calendar.token-encryption-key:}") String encodedKey) {
        this.key = decodeKey(encodedKey);
    }

    public boolean isConfigured() {
        return key != null;
    }

    public String encrypt(String value) {
        requireConfigured();
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] result = Arrays.copyOf(iv, iv.length + encrypted.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Google Calendar 토큰 암호화에 실패했습니다.", exception);
        }
    }

    public String decrypt(String value) {
        requireConfigured();
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            if (decoded.length <= IV_LENGTH) throw new GeneralSecurityException("invalid encrypted token");
            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(decoded, IV_LENGTH, decoded.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Google Calendar 토큰 복호화에 실패했습니다.", exception);
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new BusinessException("GOOGLE_CALENDAR_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE,
                    "Google Calendar 토큰 암호화 키가 설정되지 않았습니다.");
        }
    }

    private byte[] decodeKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedKey.trim());
            if (decoded.length != 32) {
                throw new IllegalArgumentException("GOOGLE_TOKEN_ENCRYPTION_KEY는 Base64로 인코딩한 32바이트 키여야 합니다.");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("GOOGLE_TOKEN_ENCRYPTION_KEY 설정을 확인해 주세요.", exception);
        }
    }
}
