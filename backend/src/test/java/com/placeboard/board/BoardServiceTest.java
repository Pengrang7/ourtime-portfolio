package com.placeboard.board;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoardServiceTest {
    private final BoardMapper boardMapper = mock(BoardMapper.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private BoardService boardService;

    @BeforeEach
    void setUp() {
        boardService = new BoardService(boardMapper, currentUserService);
        when(currentUserService.resolve("manager@example.com", "관리자"))
                .thenReturn(new UserSummary(5L, "manager@example.com", "관리자"));
        when(boardMapper.findAccess(1L, 5L))
                .thenReturn(new BoardAccess(1L, 2L, false, false, false, true));
    }

    @Test
    void memberWithInvitePermissionCanUpdateExistingMemberPermissions() {
        MemberPermissionRequest request = new MemberPermissionRequest(true, true, false, false);
        BoardMemberResponse response = mock(BoardMemberResponse.class);
        when(boardMapper.updateMemberPermissions(1L, 7L, request)).thenReturn(1);
        when(boardMapper.findMember(1L, 7L)).thenReturn(response);

        BoardMemberResponse updated = boardService.updateMemberPermissions(
                1L, 7L, request, "manager@example.com", "관리자");

        assertThat(updated).isSameAs(response);
        verify(boardMapper).updateMemberPermissions(1L, 7L, request);
    }

    @Test
    void memberCannotUpdateOwnPermissions() {
        MemberPermissionRequest request = new MemberPermissionRequest(true, true, true, true);

        assertThatThrownBy(() -> boardService.updateMemberPermissions(
                1L, 5L, request, "manager@example.com", "관리자"))
                .hasMessage("자신의 권한은 수정할 수 없습니다.");

        verify(boardMapper, never()).updateMemberPermissions(anyLong(), anyLong(), any());
    }
}
