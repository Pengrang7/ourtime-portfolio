package com.placeboard.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.placeboard.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Google Identity Services 버튼이 넘겨준 ID 토큰(JWT)을 검증한다.
 * 서명(구글 공개키), 발급자, 만료, aud(우리 클라이언트 ID)를 모두 확인한다.
 */
@Component
public class GoogleTokenVerifier {
    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${placeboard.auth.google-client-id:}") String clientId) {
        this.clientId = clientId.trim();
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(this.clientId))
                .build();
    }

    public boolean isConfigured() {
        return !clientId.isEmpty();
    }

    public String clientId() {
        return clientId;
    }

    public GoogleProfile verify(String credential) {
        if (!isConfigured()) {
            throw new BusinessException("GOOGLE_LOGIN_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE,
                    "서버에 GOOGLE_CLIENT_ID가 설정되지 않았습니다.");
        }
        GoogleIdToken token;
        try {
            token = verifier.verify(credential);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
            token = null;
        }
        if (token == null) {
            throw new BusinessException("GOOGLE_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "구글 로그인 정보를 확인하지 못했습니다.");
        }

        GoogleIdToken.Payload payload = token.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new BusinessException("GOOGLE_EMAIL_NOT_VERIFIED", HttpStatus.FORBIDDEN, "인증된 이메일이 있는 구글 계정만 사용할 수 있습니다.");
        }
        return new GoogleProfile(payload.getSubject(), payload.getEmail(), (String) payload.get("name"));
    }

    public record GoogleProfile(String subject, String email, String name) {
    }
}
