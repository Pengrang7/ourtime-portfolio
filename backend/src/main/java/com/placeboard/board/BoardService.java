package com.placeboard.board;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import com.placeboard.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardService {
    private final BoardMapper boardMapper;
    private final CurrentUserService currentUserService;

    public BoardService(BoardMapper boardMapper, CurrentUserService currentUserService) {
        this.boardMapper = boardMapper;
        this.currentUserService = currentUserService;
    }

    public List<BoardSummary> findBoards(String email, String nickname) {
        return boardMapper.findAllAccessible(currentUserService.resolve(email, nickname).userId());
    }

    @Transactional
    public BoardSummary create(BoardCreateRequest request, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardMapper.insert(request.name().trim(), request.mapType(), user.userId());
        long boardId = boardMapper.lastInsertId();
        return new BoardSummary(boardId, request.name().trim(), request.mapType(), user.userId(), user.nickname());
    }

    @Transactional
    public BoardSummary rename(long boardId, BoardNameRequest request, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        BoardAccess access = requireAccess(boardId, user.userId());
        if (!access.isOwner(user.userId())) {
            throw new BusinessException("BOARD_RENAME_FORBIDDEN", HttpStatus.FORBIDDEN, "보드 소유자만 이름을 바꿀 수 있습니다.");
        }
        String name = request.name().trim();
        boardMapper.rename(boardId, name);
        return new BoardSummary(boardId, name, access.mapType(), user.userId(), user.nickname());
    }

    public BoardAccess requireAccess(long boardId, long userId) {
        BoardAccess access = boardMapper.findAccess(boardId, userId);
        if (access == null) {
            throw new BusinessException("BOARD_ACCESS_DENIED", HttpStatus.FORBIDDEN, "보드에 접근할 권한이 없습니다.");
        }
        return access;
    }

    @Transactional
    public BoardMemberResponse invite(long boardId, MemberInviteRequest request, String email, String nickname) {
        UserSummary actor = currentUserService.resolve(email, nickname);
        BoardAccess access = requireAccess(boardId, actor.userId());
        if (!access.canInviteMember()) {
            throw new BusinessException("MEMBER_INVITE_FORBIDDEN", HttpStatus.FORBIDDEN, "멤버 초대 권한이 없습니다.");
        }

        UserSummary invited = currentUserService.findOrInvite(request.email());
        if (access.ownerId() == invited.userId()) {
            throw new BusinessException("OWNER_ALREADY_MEMBER", HttpStatus.CONFLICT, "보드 소유자는 이미 멤버입니다.");
        }
        boardMapper.upsertMember(boardId, invited.userId(), request);
        BoardMemberResponse member = boardMapper.findMember(boardId, invited.userId());
        if (member == null) {
            throw new BusinessException("MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "초대한 멤버를 찾을 수 없습니다.");
        }
        return member;
    }

    public List<BoardMemberResponse> findMembers(long boardId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        requireAccess(boardId, user.userId());
        return boardMapper.findMembers(boardId);
    }

    @Transactional
    public BoardMemberResponse updateMemberPermissions(long boardId, long memberUserId,
                                                       MemberPermissionRequest request,
                                                       String email, String nickname) {
        UserSummary actor = currentUserService.resolve(email, nickname);
        BoardAccess access = requireAccess(boardId, actor.userId());
        if (!access.canInviteMember()) {
            throw new BusinessException("MEMBER_MANAGE_FORBIDDEN", HttpStatus.FORBIDDEN,
                    "멤버 권한을 수정할 권한이 없습니다.");
        }
        if (access.ownerId() == memberUserId) {
            throw new BusinessException("OWNER_PERMISSION_IMMUTABLE", HttpStatus.CONFLICT,
                    "보드 소유자의 권한은 수정할 수 없습니다.");
        }
        if (actor.userId() == memberUserId) {
            throw new BusinessException("SELF_PERMISSION_IMMUTABLE", HttpStatus.CONFLICT,
                    "자신의 권한은 수정할 수 없습니다.");
        }
        if (boardMapper.updateMemberPermissions(boardId, memberUserId, request) == 0) {
            throw new BusinessException("MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다.");
        }
        return boardMapper.findMember(boardId, memberUserId);
    }

    @Transactional
    public void delete(long boardId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        BoardAccess access = requireAccess(boardId, user.userId());
        if (!access.isOwner(user.userId())) {
            throw new BusinessException("BOARD_DELETE_FORBIDDEN", HttpStatus.FORBIDDEN, "보드 소유자만 보드를 삭제할 수 있습니다.");
        }
        if (boardMapper.softDelete(boardId, user.userId()) == 0) {
            throw new BusinessException("BOARD_NOT_FOUND", HttpStatus.NOT_FOUND, "보드를 찾을 수 없습니다.");
        }
    }
}
