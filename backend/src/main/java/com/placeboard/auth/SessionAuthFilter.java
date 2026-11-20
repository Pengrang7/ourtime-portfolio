package com.placeboard.auth;

import tools.jackson.databind.ObjectMapper;
import com.placeboard.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;

/**
 * 로그인 세션의 사용자를 API 요청의 현재 사용자로 정한다.
 * 컨트롤러들은 X-User-Email/X-User-Nickname 헤더로 사용자를 받으므로, 세션이 있으면 두 헤더를 세션 사용자 값으로 덮어써
 * 클라이언트가 보낸 헤더로 다른 사람을 흉내 낼 수 없게 한다.
 * 세션이 없으면 401을 주되, 로컬 개발용 header-login-enabled가 켜져 있으면 기존 헤더 방식 그대로 통과시킨다.
 */
@Component
public class SessionAuthFilter extends OncePerRequestFilter {
    public static final String SESSION_USER_ID = "placeboard.userId";
    static final String CURRENT_USER_ATTRIBUTE = SessionAuthFilter.class.getName() + ".currentUser";
    static final String EMAIL_HEADER = "X-User-Email";
    static final String NICKNAME_HEADER = "X-User-Nickname";

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final boolean headerLoginEnabled;

    public SessionAuthFilter(UserMapper userMapper, ObjectMapper objectMapper,
                             @Value("${placeboard.auth.header-login-enabled:false}") boolean headerLoginEnabled) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.headerLoginEnabled = headerLoginEnabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/auth/")
                || path.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UserSummary user = sessionUser(request);
        if (user != null) {
            request.setAttribute(CURRENT_USER_ATTRIBUTE, user);
            chain.doFilter(new SessionUserRequest(request, user), response);
            return;
        }
        if (headerLoginEnabled) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), new ApiError("LOGIN_REQUIRED", "로그인이 필요합니다."));
    }

    private UserSummary sessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(SESSION_USER_ID) instanceof Long userId)) return null;
        UserSummary user = userMapper.findById(userId);
        if (user == null) session.invalidate();
        return user;
    }

    private static final class SessionUserRequest extends HttpServletRequestWrapper {
        private final String email;
        private final String nickname;

        SessionUserRequest(HttpServletRequest request, UserSummary user) {
            super(request);
            this.email = user.email();
            this.nickname = user.nickname() == null ? "" : URLEncoder.encode(user.nickname(), StandardCharsets.UTF_8);
        }

        @Override
        public String getHeader(String name) {
            if (EMAIL_HEADER.equalsIgnoreCase(name)) return email;
            if (NICKNAME_HEADER.equalsIgnoreCase(name)) return nickname;
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (EMAIL_HEADER.equalsIgnoreCase(name) || NICKNAME_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(Collections.singletonList(getHeader(name)));
            }
            return super.getHeaders(name);
        }
    }
}
