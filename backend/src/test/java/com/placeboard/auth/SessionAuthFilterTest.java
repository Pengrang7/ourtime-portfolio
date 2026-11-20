package com.placeboard.auth;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionAuthFilterTest {
    private final UserMapper userMapper = mock(UserMapper.class);

    @Test
    void sessionUserOverridesSpoofedHeaders() throws Exception {
        when(userMapper.findById(7L)).thenReturn(new UserSummary(7L, "member@example.com", "철수"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/boards");
        request.addHeader("X-User-Email", "victim@example.com");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthFilter.SESSION_USER_ID, 7L);
        request.setSession(session);
        MockFilterChain chain = new MockFilterChain();

        filter(false).doFilter(request, new MockHttpServletResponse(), chain);

        HttpServletRequest passed = (HttpServletRequest) chain.getRequest();
        assertThat(passed.getHeader("X-User-Email")).isEqualTo("member@example.com");
        assertThat(passed.getHeaders("x-user-email").nextElement()).isEqualTo("member@example.com");
    }

    @Test
    void rejectsRequestWithoutSession() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(false).doFilter(new MockHttpServletRequest("GET", "/api/boards"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("LOGIN_REQUIRED");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void headerLoginModePassesRequestThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/boards");
        request.addHeader("X-User-Email", "dev@local.place");
        MockFilterChain chain = new MockFilterChain();

        filter(true).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(((HttpServletRequest) chain.getRequest()).getHeader("X-User-Email")).isEqualTo("dev@local.place");
    }

    @Test
    void authEndpointsAreOpen() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(false).doFilter(new MockHttpServletRequest("POST", "/api/auth/google"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    private SessionAuthFilter filter(boolean headerLoginEnabled) {
        return new SessionAuthFilter(userMapper, new ObjectMapper(), headerLoginEnabled);
    }
}
