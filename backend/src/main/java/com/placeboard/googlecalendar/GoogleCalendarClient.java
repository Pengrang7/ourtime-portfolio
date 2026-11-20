package com.placeboard.googlecalendar;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HexFormat;
import java.util.function.Supplier;

/** 일정 동기화와 생리 주기 동기화가 함께 쓰는 Google Calendar REST 호출 모음. */
@Component
class GoogleCalendarClient {
    private static final int MAX_RETRIES = 4;

    private final GoogleTokenCipher tokenCipher;
    private final RestClient restClient = RestClient.create();
    private final String clientId;
    private final String clientSecret;

    GoogleCalendarClient(
            GoogleTokenCipher tokenCipher,
            @Value("${placeboard.auth.google-client-id:}") String clientId,
            @Value("${placeboard.google-calendar.client-secret:}") String clientSecret) {
        this.tokenCipher = tokenCipher;
        this.clientId = clientId.trim();
        this.clientSecret = clientSecret.trim();
    }

    String accessToken(GoogleCalendarConnection connection) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", tokenCipher.decrypt(connection.encryptedRefreshToken()));
        form.add("grant_type", "refresh_token");
        TokenResponse token = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (token == null || token.accessToken() == null) {
            throw new IllegalStateException("Google Calendar access token 응답이 없습니다.");
        }
        return token.accessToken();
    }

    /**
     * 결정적 ID 로 이벤트를 만들고, 이미 같은 ID 가 있으면(이전에 삭제된 이벤트 포함) 덮어쓴다.
     * 실제 저장된 이벤트 ID 를 돌려준다.
     */
    String insertOrUpdate(String calendarId, String eventId, String accessToken, Object body) {
        try {
            EventResponse response = withRetry(() -> restClient.post()
                    .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events", calendarId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(EventResponse.class));
            return response != null && response.id() != null ? response.id() : eventId;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 409) throw exception;
            update(calendarId, eventId, accessToken, body);
            return eventId;
        }
    }

    void update(String calendarId, String eventId, String accessToken, Object body) {
        withRetry(() -> restClient.put()
                .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events/{eventId}",
                        calendarId, eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity());
    }

    /** 사용자가 Google 쪽에서 먼저 지운 이벤트(404/410)는 이미 삭제된 것으로 본다. */
    void delete(String calendarId, String eventId, String accessToken) {
        try {
            withRetry(() -> restClient.delete()
                    .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events/{eventId}",
                            calendarId, eventId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity());
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404 && exception.getStatusCode().value() != 410) throw exception;
        }
    }

    /**
     * 연동 직후처럼 한꺼번에 많이 보내면 Google 이 "너무 빠름"(429, 403 rateLimitExceeded)이나 일시 오류(5xx)를 돌려준다.
     * 이때는 1·2·4·8초 쉬었다가 다시 보낸다.
     */
    private <T> T withRetry(Supplier<T> call) {
        for (int attempt = 0; ; attempt++) {
            try {
                return call.get();
            } catch (RestClientResponseException exception) {
                if (attempt >= MAX_RETRIES || !isRetryable(exception.getStatusCode().value(),
                        exception.getResponseBodyAsString())) {
                    throw exception;
                }
                sleep(1000L << attempt);
            }
        }
    }

    static boolean isRetryable(int status, String body) {
        if (status == 429 || status >= 500) return true;
        return status == 403 && body != null
                && (body.contains("rateLimitExceeded") || body.contains("userRateLimitExceeded"));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    /** Google 이벤트 ID 규칙(base32hex 문자, 5~1024자)을 만족하는 결정적 ID. */
    static String deterministicEventId(String prefix, String source) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return prefix + HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    private record EventResponse(String id) {
    }
}
