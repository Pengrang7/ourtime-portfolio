package com.placeboard.googlecalendar;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/google-calendar")
public class GoogleCalendarController {
    private final GoogleCalendarOAuthService oauthService;
    private final CurrentUserService currentUserService;

    public GoogleCalendarController(GoogleCalendarOAuthService oauthService, CurrentUserService currentUserService) {
        this.oauthService = oauthService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/status")
    public GoogleCalendarOAuthService.GoogleCalendarStatus status(
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        return oauthService.status(user.userId());
    }

    @GetMapping("/connect")
    public ResponseEntity<Void> connect(
            HttpSession session,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        return ResponseEntity.status(HttpStatus.FOUND).location(oauthService.authorizationUri(session, user)).build();
    }

    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String code,
            @RequestParam String state,
            HttpSession session,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        oauthService.connect(currentUserService.resolve(email, nickname), session, code, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/?googleCalendar=connected"))
                .build();
    }

    @DeleteMapping("/connection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        oauthService.disconnect(currentUserService.resolve(email, nickname).userId());
    }
}
