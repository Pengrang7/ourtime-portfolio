package com.placeboard.googlecalendar;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.placeboard.auth.UserSummary;
import com.placeboard.common.BusinessException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class GoogleCalendarOAuthService {
    static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.events";
    private static final String STATE_SESSION_KEY = "placeboard.googleCalendarOAuthState";

    private final GoogleCalendarConnectionMapper mapper;
    private final GoogleTokenCipher tokenCipher;
    private final RestClient restClient;
    private final ApplicationEventPublisher eventPublisher;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final SecureRandom secureRandom = new SecureRandom();

    public GoogleCalendarOAuthService(
            GoogleCalendarConnectionMapper mapper,
            GoogleTokenCipher tokenCipher,
            ApplicationEventPublisher eventPublisher,
            @Value("${placeboard.auth.google-client-id:}") String clientId,
            @Value("${placeboard.google-calendar.client-secret:}") String clientSecret,
            @Value("${placeboard.google-calendar.redirect-uri:}") String redirectUri) {
        this.mapper = mapper;
        this.tokenCipher = tokenCipher;
        this.eventPublisher = eventPublisher;
        this.restClient = RestClient.create();
        this.clientId = clientId.trim();
        this.clientSecret = clientSecret.trim();
        this.redirectUri = redirectUri.trim();
    }

    public GoogleCalendarStatus status(long userId) {
        return new GoogleCalendarStatus(isConfigured(), mapper.findConnection(userId) != null);
    }

    public URI authorizationUri(HttpSession session, UserSummary user) {
        requireConfigured();
        byte[] stateBytes = new byte[32];
        secureRandom.nextBytes(stateBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);
        session.setAttribute(STATE_SESSION_KEY, new OAuthState(state, user.userId()));
        return URI.create("https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode(CALENDAR_SCOPE)
                + "&access_type=offline&include_granted_scopes=true&prompt=consent"
                + "&login_hint=" + encode(user.email())
                + "&state=" + encode(state));
    }

    @Transactional
    public void connect(UserSummary user, HttpSession session, String code, String state) {
        requireConfigured();
        Object expected = session.getAttribute(STATE_SESSION_KEY);
        session.removeAttribute(STATE_SESSION_KEY);
        if (!(expected instanceof OAuthState expectedState) || expectedState.userId() != user.userId()
                || state == null || !constantTimeEquals(expectedState.value(), state)) {
            throw new BusinessException("GOOGLE_CALENDAR_STATE_INVALID", HttpStatus.BAD_REQUEST,
                    "Google Calendar 연결 요청이 만료되었거나 올바르지 않습니다.");
        }

        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", redirectUri);
        TokenResponse token = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (token == null || token.refreshToken() == null || token.refreshToken().isBlank()) {
            throw new BusinessException("GOOGLE_CALENDAR_REFRESH_TOKEN_MISSING", HttpStatus.BAD_GATEWAY,
                    "Google Calendar 장기 연결 토큰을 받지 못했습니다. 다시 연결해 주세요.");
        }
        mapper.upsertConnection(user.userId(), tokenCipher.encrypt(token.refreshToken()));
        eventPublisher.publishEvent(new GoogleCalendarConnectedEvent(user.userId()));
    }

    @Transactional
    public void disconnect(long userId) {
        mapper.disableConnection(userId);
    }

    public boolean isConfigured() {
        return !clientId.isEmpty() && !clientSecret.isEmpty() && !redirectUri.isEmpty() && tokenCipher.isConfigured();
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new BusinessException("GOOGLE_CALENDAR_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE,
                    "Google Calendar 서버 설정이 완료되지 않았습니다.");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    public record GoogleCalendarStatus(boolean configured, boolean connected) {
    }

    private record TokenResponse(@JsonProperty("refresh_token") String refreshToken) {
    }

    private record OAuthState(String value, long userId) implements java.io.Serializable {
    }
}
