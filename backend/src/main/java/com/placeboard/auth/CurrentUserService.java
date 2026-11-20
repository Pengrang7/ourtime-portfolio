package com.placeboard.auth;

import com.placeboard.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Locale;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
public class CurrentUserService {
    public static final String DEFAULT_EMAIL = "owner@local.place";
    public static final String DEFAULT_NICKNAME = "로컬 사용자";

    private final UserMapper userMapper;

    public CurrentUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Transactional
    public UserSummary resolve(String email, String nickname) {
        UserSummary authenticated = authenticatedUser();
        if (authenticated != null) return authenticated;

        String normalizedEmail = normalizeEmail(email);
        UserSummary user = userMapper.findByEmail(normalizedEmail);
        // 닉네임이 비어 있는(초대만 받은) 사용자도 그대로 돌려준다. 닉네임은 본인이 updateNickname으로 정한다.
        if (user != null) return user;

        // 헤더 로그인(로컬 개발) 모드에서 처음 보는 이메일이면 바로 만든다.
        String decodedNickname = nickname == null ? "" : URLDecoder.decode(nickname, StandardCharsets.UTF_8);
        String normalizedNickname = decodedNickname.isBlank() ? normalizedEmail : decodedNickname.trim();
        userMapper.upsertLocalUser(normalizedEmail, normalizedNickname);
        return userMapper.findByEmail(normalizedEmail);
    }

    @Transactional
    public UserSummary updateNickname(String email, String nickname, String newNickname) {
        UserSummary user = resolve(email, nickname);
        userMapper.updateNickname(user.userId(), newNickname.trim());
        return userMapper.findById(user.userId());
    }

    // 구글 로그인: google_sub → 이메일 순서로 기존 사용자를 찾고, 없으면 닉네임 없이 만든다.
    @Transactional
    public UserSummary loginWithGoogle(String googleSubject, String email) {
        UserSummary user = userMapper.findByGoogleSub(googleSubject);
        if (user != null) return user;

        String normalizedEmail = normalizeEmail(email);
        UserSummary byEmail = userMapper.findByEmail(normalizedEmail);
        if (byEmail == null) {
            userMapper.insertGoogleUser(normalizedEmail, googleSubject);
        } else if (userMapper.linkGoogleSub(byEmail.userId(), googleSubject) == 0) {
            throw new BusinessException("GOOGLE_ACCOUNT_CONFLICT", HttpStatus.CONFLICT,
                    "이 이메일은 이미 다른 구글 계정과 연결되어 있습니다.");
        }
        return userMapper.findByGoogleSub(googleSubject);
    }

    // 초대받은 사람의 닉네임은 본인이 첫 접속할 때 정하므로 비워 둔다.
    @Transactional
    public UserSummary findOrInvite(String email) {
        String normalizedEmail = normalizeEmail(email);
        UserSummary user = userMapper.findByEmail(normalizedEmail);
        if (user != null) return user;
        userMapper.insertPendingUser(normalizedEmail);
        return userMapper.findByEmail(normalizedEmail);
    }

    private String normalizeEmail(String email) {
        String value = email == null || email.isBlank() ? DEFAULT_EMAIL : email.trim().toLowerCase(Locale.ROOT);
        if (!value.contains("@") || value.length() > 320) {
            throw new BusinessException("INVALID_EMAIL", HttpStatus.BAD_REQUEST, "올바른 이메일을 입력해 주세요.");
        }
        return value;
    }

    private UserSummary authenticatedUser() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) return null;
        Object value = attributes.getAttribute(SessionAuthFilter.CURRENT_USER_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST);
        return value instanceof UserSummary user ? user : null;
    }
}
