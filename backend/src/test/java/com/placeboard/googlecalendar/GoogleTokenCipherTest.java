package com.placeboard.googlecalendar;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleTokenCipherTest {
    @Test
    void encryptsRefreshTokenWithRandomIvAndDecryptsIt() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        GoogleTokenCipher cipher = new GoogleTokenCipher(key);

        String first = cipher.encrypt("refresh-token");
        String second = cipher.encrypt("refresh-token");

        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("refresh-token");
        assertThat(cipher.decrypt(second)).isEqualTo("refresh-token");
    }

    @Test
    void rejectsInvalidKeyLengthAtStartup() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new GoogleTokenCipher(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GOOGLE_TOKEN_ENCRYPTION_KEY 설정을 확인해 주세요.");
    }
}
