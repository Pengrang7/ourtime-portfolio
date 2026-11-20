package com.placeboard.schedule;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import com.placeboard.board.BoardAccess;
import com.placeboard.board.BoardService;
import com.placeboard.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {
    private final ScheduleMapper scheduleMapper = mock(ScheduleMapper.class);
    private final BoardService boardService = mock(BoardService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(scheduleMapper, boardService, currentUserService, eventPublisher);
        when(currentUserService.resolve("user@example.com", "사용자"))
                .thenReturn(new UserSummary(7L, "user@example.com", "사용자"));
        when(boardService.requireAccess(1L, 7L))
                .thenReturn(new BoardAccess(1L, 7L, true, true, true, true));
    }

    @Test
    void publishesBoardEventAfterCreatingSchedule() {
        ScheduleRequest request = new ScheduleRequest(
                3L, "데이트",
                LocalDateTime.of(2026, 9, 30, 10, 0),
                LocalDateTime.of(2026, 9, 30, 11, 0),
                false,
                "메모",
                List.of(),
                "#0B8043"
        );
        ScheduleResponse response = mock(ScheduleResponse.class);
        when(scheduleMapper.insert(1L, request, "데이트", "메모", "#0B8043", 7L)).thenReturn(1);
        when(scheduleMapper.lastInsertId()).thenReturn(9L);
        when(scheduleMapper.findById(9L)).thenReturn(response);

        ScheduleResponse created = scheduleService.create(1L, request, "user@example.com", "사용자");

        assertThat(created).isSameAs(response);
        verify(eventPublisher).publishEvent(new BoardSchedulesChangedEvent(1L, 7L));
    }

    @Test
    void rejectsQueryRangeLongerThanExactlySixtyTwoDays() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);

        assertThatThrownBy(() -> scheduleService.findAll(
                1L, from, from.plusDays(62).plusSeconds(1), "user@example.com", "사용자"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("조회 기간은 시작보다 늦은 62일 이내여야 합니다.");
    }
}
