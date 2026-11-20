package com.placeboard.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final GoogleTokenVerifier googleTokenVerifier;
    private final CurrentUserService currentUserService;
    private final boolean headerLoginEnabled;

    public AuthController(GoogleTokenVerifier googleTokenVerifier, CurrentUserService currentUserService,
                          @Value("${placeboard.auth.header-login-enabled:false}") boolean headerLoginEnabled) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.currentUserService = currentUserService;
        this.headerLoginEnabled = headerLoginEnabled;
    }

    // 프론트는 클라이언트 ID를 따로 설정하지 않고 여기서 받아 구글 버튼을 그린다.
    @GetMapping("/config")
    public AuthConfig config() {
        return new AuthConfig(googleTokenVerifier.isConfigured() ? googleTokenVerifier.clientId() : null, headerLoginEnabled);
    }

    @PostMapping("/google")
    public LoginResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest httpRequest) {
        GoogleTokenVerifier.GoogleProfile profile = googleTokenVerifier.verify(request.credential());
        UserSummary user = currentUserService.loginWithGoogle(profile.subject(), profile.email());

        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId(); // 로그인 전 세션 ID를 재사용하지 않는다(세션 고정 방지).
        session.setAttribute(SessionAuthFilter.SESSION_USER_ID, user.userId());
        return new LoginResponse(user, profile.name());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) session.invalidate();
    }

    public record AuthConfig(String googleClientId, boolean headerLoginEnabled) {
    }

    public record GoogleLoginRequest(@NotBlank String credential) {
    }

    // suggestedNickname: 닉네임이 없는 첫 로그인 때 입력칸에 미리 채워 줄 구글 프로필 이름
    public record LoginResponse(UserSummary user, String suggestedNickname) {
    }
}
