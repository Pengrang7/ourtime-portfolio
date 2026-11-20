package com.placeboard.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class CurrentUserServiceTest {
    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void reusesAuthenticatedUserWithoutQueryingAgain() {
        UserMapper userMapper = mock(UserMapper.class);
        CurrentUserService service = new CurrentUserService(userMapper);
        UserSummary authenticated = new UserSummary(7L, "member@example.com", "멤버");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SessionAuthFilter.CURRENT_USER_ATTRIBUTE, authenticated);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UserSummary resolved = service.resolve("ignored@example.com", "ignored");

        assertThat(resolved).isSameAs(authenticated);
        verifyNoInteractions(userMapper);
    }
}
