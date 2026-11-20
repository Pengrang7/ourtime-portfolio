package com.placeboard.googlecalendar;

import com.placeboard.schedule.ScheduleMapper;
import com.placeboard.schedule.ScheduleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleCalendarSyncServiceTest {
    private final ScheduleMapper scheduleMapper = mock(ScheduleMapper.class);
    private final GoogleCalendarConnectionMapper connectionMapper = mock(GoogleCalendarConnectionMapper.class);
    private final GoogleCalendarClient client = mock(GoogleCalendarClient.class);
    private GoogleCalendarSyncService service;

    @BeforeEach
    void setUp() {
        service = new GoogleCalendarSyncService(scheduleMapper, connectionMapper, client);
    }

    @Test
    void 앱의_일정_색을_Google_색_번호로_바꾼다() {
        assertThat(GoogleCalendarSyncService.googleColorId("#0B8043")).isEqualTo("10");
        assertThat(GoogleCalendarSyncService.googleColorId("#d50000")).isEqualTo("11");
        assertThat(GoogleCalendarSyncService.googleColorId("#168A4A")).isNull();
        assertThat(GoogleCalendarSyncService.googleColorId(null)).isNull();
    }

    @Test
    void 공유_일정을_연동한_모든_멤버의_Google_Calendar에_등록한다() {
        ScheduleResponse schedule = schedule(11L);
        when(scheduleMapper.findById(11L)).thenReturn(schedule);
        when(connectionMapper.findConnectedUserIdsForSchedule(11L)).thenReturn(List.of(7L, 8L));
        when(connectionMapper.findConnection(7L)).thenReturn(connection(7L));
        when(connectionMapper.findConnection(8L)).thenReturn(connection(8L));
        when(client.accessToken(any())).thenReturn("access-token");
        when(client.insertOrUpdate(eq("primary"), anyString(), eq("access-token"), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        service.sync(new ScheduleGoogleSyncRequestedEvent(11L,
                ScheduleGoogleSyncRequestedEvent.Action.UPSERT));

        verify(connectionMapper).upsertEvent(eq(11L), eq(7L), anyString());
        verify(connectionMapper).upsertEvent(eq(11L), eq(8L), anyString());
    }

    @Test
    void 삭제된_공유_일정을_동기화했던_모든_멤버의_Google_Calendar에서_삭제한다() {
        when(connectionMapper.findSyncedUserIds(11L)).thenReturn(List.of(7L, 8L));
        when(connectionMapper.findEventId(11L, 7L)).thenReturn("event-7");
        when(connectionMapper.findEventId(11L, 8L)).thenReturn("event-8");
        when(connectionMapper.findConnection(7L)).thenReturn(connection(7L));
        when(connectionMapper.findConnection(8L)).thenReturn(connection(8L));
        when(client.accessToken(any())).thenReturn("access-token");

        service.sync(new ScheduleGoogleSyncRequestedEvent(11L,
                ScheduleGoogleSyncRequestedEvent.Action.DELETE));

        verify(client).delete("primary", "event-7", "access-token");
        verify(client).delete("primary", "event-8", "access-token");
        verify(connectionMapper).deleteEvent(11L, 7L);
        verify(connectionMapper).deleteEvent(11L, 8L);
    }

    @Test
    void Google_Calendar_재연결_시_과거에_삭제되지_못한_일정을_정리한다() {
        when(connectionMapper.findStaleScheduleIds(7L)).thenReturn(List.of(11L));
        when(connectionMapper.findEventId(11L, 7L)).thenReturn("stale-event");
        when(connectionMapper.findConnection(7L)).thenReturn(connection(7L));
        when(client.accessToken(any())).thenReturn("access-token");
        when(scheduleMapper.findActiveIdsAccessibleByUser(7L)).thenReturn(List.of());

        service.syncExistingSchedules(new GoogleCalendarConnectedEvent(7L));

        verify(client).delete("primary", "stale-event", "access-token");
        verify(connectionMapper).deleteEvent(11L, 7L);
    }

    private GoogleCalendarConnection connection(long userId) {
        return new GoogleCalendarConnection(userId, "encrypted-refresh-token", "primary");
    }

    private ScheduleResponse schedule(long scheduleId) {
        LocalDateTime start = LocalDateTime.of(2026, 11, 20, 10, 0);
        return new ScheduleResponse(scheduleId, 1L, null, "데이트", null, null,
                null, null, start, start.plusHours(1), false, null, "#0B8043",
                null, null, null, 7L, "사용자", start, start);
    }
}
