package com.placeboard.cycle;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import com.placeboard.board.BoardService;
import com.placeboard.common.BusinessException;
import com.placeboard.googlecalendar.CycleGoogleSyncRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CyclePeriodService {
    private final CyclePeriodMapper cyclePeriodMapper;
    private final CurrentUserService currentUserService;
    private final BoardService boardService;
    private final ApplicationEventPublisher eventPublisher;

    public CyclePeriodService(CyclePeriodMapper cyclePeriodMapper, CurrentUserService currentUserService,
                              BoardService boardService,
                              ApplicationEventPublisher eventPublisher) {
        this.cyclePeriodMapper = cyclePeriodMapper;
        this.currentUserService = currentUserService;
        this.boardService = boardService;
        this.eventPublisher = eventPublisher;
    }

    public List<CyclePeriodResponse> findMine(String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        return cyclePeriodMapper.findAll(user.userId());
    }

    public List<BoardCyclePeriodResponse> findForBoard(long boardId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        return cyclePeriodMapper.findAllForBoard(boardId);
    }

    @Transactional
    public CyclePeriodResponse create(CyclePeriodRequest request, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        try {
            cyclePeriodMapper.insert(user.userId(), request);
        } catch (DuplicateKeyException exception) {
            throw duplicateStartDate();
        }
        publishChanges(user.userId());
        return cyclePeriodMapper.findById(cyclePeriodMapper.lastInsertId(), user.userId());
    }

    @Transactional
    public CyclePeriodResponse update(long periodId, CyclePeriodRequest request, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        try {
            if (cyclePeriodMapper.update(periodId, user.userId(), request) == 0) throw notFound();
        } catch (DuplicateKeyException exception) {
            throw duplicateStartDate();
        }
        publishChanges(user.userId());
        return cyclePeriodMapper.findById(periodId, user.userId());
    }

    @Transactional
    public void delete(long periodId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        if (cyclePeriodMapper.delete(periodId, user.userId()) == 0) throw notFound();
        publishChanges(user.userId());
    }

    private void publishChanges(long userId) {
        eventPublisher.publishEvent(new CycleGoogleSyncRequestedEvent(userId));
        cyclePeriodMapper.findBoardIdsForUser(userId)
                .forEach(boardId -> eventPublisher.publishEvent(new BoardCyclePeriodsChangedEvent(boardId, userId)));
    }

    private BusinessException duplicateStartDate() {
        return new BusinessException("CYCLE_PERIOD_DUPLICATE", HttpStatus.CONFLICT,
                "같은 시작일의 생리 기록이 이미 있습니다.");
    }

    private BusinessException notFound() {
        return new BusinessException("CYCLE_PERIOD_NOT_FOUND", HttpStatus.NOT_FOUND,
                "생리 기록을 찾을 수 없습니다.");
    }
}
