package com.placeboard.tag;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import com.placeboard.board.BoardAccess;
import com.placeboard.board.BoardService;
import com.placeboard.common.BusinessException;
import com.placeboard.place.BoardPlacesChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {
    private final TagMapper tagMapper;
    private final BoardService boardService;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    public TagService(TagMapper tagMapper, BoardService boardService, CurrentUserService currentUserService,
                      ApplicationEventPublisher eventPublisher) {
        this.tagMapper = tagMapper;
        this.boardService = boardService;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }

    public List<TagResponse> findAll(long boardId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        return tagMapper.findAll(boardId);
    }

    @Transactional
    public TagResponse create(long boardId, TagRequest request, String email, String nickname) {
        UserSummary user = requireManager(boardId, email, nickname);
        String name = request.name().trim();
        tagMapper.upsert(boardId, name, normalizeIcon(request.icon()), user.userId());
        TagResponse created = tagMapper.findByName(boardId, name);
        eventPublisher.publishEvent(new BoardTagsChangedEvent(boardId, user.userId()));
        return created;
    }

    @Transactional
    public void update(long boardId, long tagId, TagRequest request, String email, String nickname) {
        UserSummary user = requireManager(boardId, email, nickname);
        try {
            if (tagMapper.update(boardId, tagId, request.name().trim(), normalizeIcon(request.icon())) == 0) throw notFound();
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("TAG_DUPLICATED", HttpStatus.CONFLICT, "같은 이름의 태그가 이미 있습니다.");
        }
        eventPublisher.publishEvent(new BoardTagsChangedEvent(boardId, user.userId()));
    }

    @Transactional
    public void delete(long boardId, long tagId, String email, String nickname) {
        UserSummary user = requireManager(boardId, email, nickname);
        if (tagMapper.softDelete(boardId, tagId) == 0) throw notFound();
        tagMapper.deactivateAssignments(tagId);
        tagMapper.deactivateScheduleAssignments(tagId);
        eventPublisher.publishEvent(new BoardTagsChangedEvent(boardId, user.userId()));
    }

    @Transactional
    public void assign(long boardId, long boardPlaceId, long tagId, String email, String nickname) {
        UserSummary user = requireMember(boardId, email, nickname);
        if (tagMapper.assign(boardId, boardPlaceId, tagId, user.userId()) == 0) throw notFound();
        eventPublisher.publishEvent(new BoardPlacesChangedEvent(boardId, user.userId()));
    }

    @Transactional
    public void unassign(long boardId, long boardPlaceId, long tagId, String email, String nickname) {
        UserSummary user = requireMember(boardId, email, nickname);
        if (tagMapper.unassign(boardId, boardPlaceId, tagId) == 0) throw notFound();
        eventPublisher.publishEvent(new BoardPlacesChangedEvent(boardId, user.userId()));
    }

    // 장소에 기존 태그를 붙이고 떼는 것은 모든 멤버가 할 수 있다. 태그 생성·수정·삭제만 관리 권한이 필요하다.
    private UserSummary requireMember(long boardId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        return user;
    }

    private UserSummary requireManager(long boardId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        BoardAccess access = boardService.requireAccess(boardId, user.userId());
        if (!access.canManageTag()) {
            throw new BusinessException("TAG_MANAGE_FORBIDDEN", HttpStatus.FORBIDDEN, "태그 관리 권한이 없습니다.");
        }
        return user;
    }

    private String normalizeIcon(String icon) {
        return icon == null || icon.isBlank() ? null : icon.trim();
    }

    private BusinessException notFound() {
        return new BusinessException("TAG_NOT_FOUND", HttpStatus.NOT_FOUND, "태그 또는 장소를 찾을 수 없습니다.");
    }
}
