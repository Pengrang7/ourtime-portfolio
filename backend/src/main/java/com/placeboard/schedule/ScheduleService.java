package com.placeboard.schedule;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import com.placeboard.board.BoardAccess;
import com.placeboard.board.BoardService;
import com.placeboard.common.BusinessException;
import com.placeboard.googlecalendar.ScheduleGoogleSyncRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ScheduleService {
    private static final long MAX_QUERY_DAYS = 62;

    private final ScheduleMapper scheduleMapper;
    private final BoardService boardService;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    public ScheduleService(ScheduleMapper scheduleMapper, BoardService boardService,
                           CurrentUserService currentUserService, ApplicationEventPublisher eventPublisher) {
        this.scheduleMapper = scheduleMapper;
        this.boardService = boardService;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }

    public List<ScheduleResponse> findAll(long boardId, LocalDateTime from, LocalDateTime to,
                                          String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        validateQueryRange(from, to);
        return scheduleMapper.findAll(boardId, from, to);
    }

    @Transactional
    public ScheduleResponse create(long boardId, ScheduleRequest request, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        String title = normalizeTitle(request);
        if (scheduleMapper.insert(boardId, request, title, normalizeMemo(request.memo()), normalizeColor(request.color()),
                user.userId()) == 0) {
            throw placeNotFound();
        }
        long scheduleId = scheduleMapper.lastInsertId();
        assignTags(scheduleId, request.tagIds(), user.userId());
        ScheduleResponse created = scheduleMapper.findById(scheduleId);
        eventPublisher.publishEvent(new BoardSchedulesChangedEvent(boardId, user.userId()));
        eventPublisher.publishEvent(new ScheduleGoogleSyncRequestedEvent(scheduleId,
                ScheduleGoogleSyncRequestedEvent.Action.UPSERT));
        return created;
    }

    @Transactional
    public ScheduleResponse update(long boardId, long scheduleId, ScheduleRequest request,
                                   String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        BoardAccess access = boardService.requireAccess(boardId, user.userId());
        String title = normalizeTitle(request);
        if (scheduleMapper.update(boardId, scheduleId, request, title, normalizeMemo(request.memo()), normalizeColor(request.color()),
                user.userId(), access.ownerId()) == 0) {
            throw editForbidden();
        }
        scheduleMapper.clearTags(scheduleId);
        assignTags(scheduleId, request.tagIds(), user.userId());
        ScheduleResponse updated = scheduleMapper.findById(scheduleId);
        eventPublisher.publishEvent(new BoardSchedulesChangedEvent(boardId, user.userId()));
        eventPublisher.publishEvent(new ScheduleGoogleSyncRequestedEvent(scheduleId,
                ScheduleGoogleSyncRequestedEvent.Action.UPSERT));
        return updated;
    }

    @Transactional
    public void delete(long boardId, long scheduleId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        BoardAccess access = boardService.requireAccess(boardId, user.userId());
        if (scheduleMapper.softDelete(boardId, scheduleId, user.userId(), access.ownerId()) == 0) {
            throw editForbidden();
        }
        eventPublisher.publishEvent(new BoardSchedulesChangedEvent(boardId, user.userId()));
        eventPublisher.publishEvent(new ScheduleGoogleSyncRequestedEvent(scheduleId,
                ScheduleGoogleSyncRequestedEvent.Action.DELETE));
    }

    private void validateQueryRange(LocalDateTime from, LocalDateTime to) {
        if (!to.isAfter(from) || to.isAfter(from.plusDays(MAX_QUERY_DAYS))) {
            throw new BusinessException("INVALID_SCHEDULE_RANGE", HttpStatus.BAD_REQUEST,
                    "조회 기간은 시작보다 늦은 62일 이내여야 합니다.");
        }
    }

    private String normalizeMemo(String memo) {
        return memo == null || memo.isBlank() ? null : memo.trim();
    }

    private String normalizeTitle(ScheduleRequest request) {
        String title = request.title() == null ? null : request.title().trim();
        return title == null || title.isBlank() ? null : title;
    }

    private String normalizeColor(String color) {
        return color == null ? "#0B8043" : color.toUpperCase();
    }

    private void assignTags(long scheduleId, List<Long> tagIds, long userId) {
        if (tagIds == null) return;
        for (Long tagId : new LinkedHashSet<>(tagIds)) {
            if (tagId != null && scheduleMapper.assignTag(scheduleId, tagId, userId) == 0) {
                throw new BusinessException("SCHEDULE_TAG_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "보드에 등록된 태그를 찾을 수 없습니다.");
            }
        }
    }

    private BusinessException placeNotFound() {
        return new BusinessException("SCHEDULE_PLACE_NOT_FOUND", HttpStatus.NOT_FOUND,
                "보드에 저장된 장소를 찾을 수 없습니다.");
    }

    private BusinessException editForbidden() {
        return new BusinessException("SCHEDULE_EDIT_FORBIDDEN", HttpStatus.FORBIDDEN,
                "일정 작성자 또는 보드 소유자만 수정하거나 삭제할 수 있습니다.");
    }
}
