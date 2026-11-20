package com.placeboard.cycle;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import com.placeboard.board.BoardService;
import com.placeboard.googlecalendar.CycleGoogleSyncRequestedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CyclePeriodServiceTest {
    private final CyclePeriodMapper cyclePeriodMapper = mock(CyclePeriodMapper.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final BoardService boardService = mock(BoardService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final CyclePeriodService service = new CyclePeriodService(
            cyclePeriodMapper, currentUserService, boardService, eventPublisher);

    @Test
    void findsPeriodsOnlyAfterCheckingBoardAccess() {
        when(currentUserService.resolve("member@example.com", "멤버"))
                .thenReturn(new UserSummary(7L, "member@example.com", "멤버"));
        List<BoardCyclePeriodResponse> periods = List.of(mock(BoardCyclePeriodResponse.class));
        when(cyclePeriodMapper.findAllForBoard(3L)).thenReturn(periods);

        List<BoardCyclePeriodResponse> result = service.findForBoard(3L, "member@example.com", "멤버");

        assertThat(result).isSameAs(periods);
        verify(boardService).requireAccess(3L, 7L);
        verify(cyclePeriodMapper).findAllForBoard(3L);
    }

    @Test
    void publishesChangesToEveryBoardAfterCreatingPeriod() {
        UserSummary user = new UserSummary(7L, "member@example.com", "멤버");
        CyclePeriodRequest request = new CyclePeriodRequest(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-05"));
        CyclePeriodResponse response = mock(CyclePeriodResponse.class);
        when(currentUserService.resolve(user.email(), user.nickname())).thenReturn(user);
        when(cyclePeriodMapper.lastInsertId()).thenReturn(11L);
        when(cyclePeriodMapper.findById(11L, user.userId())).thenReturn(response);
        when(cyclePeriodMapper.findBoardIdsForUser(user.userId())).thenReturn(List.of(1L, 3L));

        assertThat(service.create(request, user.email(), user.nickname())).isSameAs(response);

        verify(eventPublisher).publishEvent(new CycleGoogleSyncRequestedEvent(7L));
        verify(eventPublisher).publishEvent(new BoardCyclePeriodsChangedEvent(1L, 7L));
        verify(eventPublisher).publishEvent(new BoardCyclePeriodsChangedEvent(3L, 7L));
    }
}
