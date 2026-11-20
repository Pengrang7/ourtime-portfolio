package com.placeboard.googlecalendar;

import com.placeboard.cycle.CycleCalendarEntry;
import com.placeboard.cycle.CyclePeriodMapper;
import com.placeboard.cycle.CyclePeriodResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CycleGoogleSyncServiceTest {
    private final CyclePeriodMapper cyclePeriodMapper = mock(CyclePeriodMapper.class);
    private final GoogleCalendarConnectionMapper connectionMapper = mock(GoogleCalendarConnectionMapper.class);
    private final GoogleCalendarClient client = mock(GoogleCalendarClient.class);
    private CycleGoogleSyncService service;

    @BeforeEach
    void setUp() {
        service = new CycleGoogleSyncService(cyclePeriodMapper, connectionMapper, client);
    }

    @Test
    void 종일_비공개_일정으로_만들고_종료일은_다음날로_보낸다() {
        CycleCalendarEntry entry = new CycleCalendarEntry(CycleCalendarEntry.Kind.PREDICTED_PERIOD,
                LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 13));

        CycleGoogleSyncService.CycleEventRequest request = CycleGoogleSyncService.request("pcabc", entry, "사용자");

        assertThat(request.start().date()).isEqualTo("2026-10-07");
        assertThat(request.end().date()).isEqualTo("2026-10-14");
        assertThat(request.visibility()).isEqualTo("private");
        assertThat(request.transparency()).isEqualTo("transparent");
        assertThat(request.summary()).doesNotContain("생리");
        assertThat(request.summary()).isEqualTo("사용자 🌸 예정");
        assertThat(request.colorId()).isEqualTo("1");
        assertThat(CycleGoogleSyncService.colorId(CycleCalendarEntry.Kind.FERTILE)).isEqualTo("2");
        assertThat(CycleGoogleSyncService.colorId(CycleCalendarEntry.Kind.OVULATION)).isEqualTo("5");
        assertThat(CycleGoogleSyncService.eventKey(entry)).isEqualTo("PREDICTED_PERIOD:2026-10-07:2026-10-13");
    }

    @Test
    void 이벤트_ID가_달라도_내용이_같으면_같은_지문이다() {
        CycleCalendarEntry entry = new CycleCalendarEntry(CycleCalendarEntry.Kind.FERTILE,
                LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 18));
        String hash = CycleGoogleSyncService.contentHash(CycleGoogleSyncService.request("pca", entry, "사용자"));

        assertThat(CycleGoogleSyncService.contentHash(CycleGoogleSyncService.request("pcb", entry, "사용자"))).isEqualTo(hash);
        assertThat(hash).hasSize(64);
        assertThat(CycleGoogleSyncService.contentHash(CycleGoogleSyncService.request("pca",
                new CycleCalendarEntry(CycleCalendarEntry.Kind.OVULATION, entry.startDate(), entry.endDate()), "사용자")))
                .isNotEqualTo(hash);
    }

    @Test
    void 생리_기록을_공유_보드에서_연동한_모든_사용자_캘린더에_등록한다() {
        CyclePeriodResponse period = new CyclePeriodResponse(1L,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5),
                LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0));
        when(connectionMapper.findCycleTargetUserIds(7L)).thenReturn(List.of(7L, 8L));
        when(connectionMapper.findConnection(7L)).thenReturn(connection(7L));
        when(connectionMapper.findConnection(8L)).thenReturn(connection(8L));
        when(connectionMapper.findCycleSources(7L)).thenReturn(List.of(new CycleGoogleSource(7L, "사용자")));
        when(connectionMapper.findCycleSources(8L)).thenReturn(List.of(new CycleGoogleSource(7L, "사용자")));
        when(cyclePeriodMapper.findAll(7L)).thenReturn(List.of(period));
        when(connectionMapper.findCycleEvents(7L)).thenReturn(List.of());
        when(connectionMapper.findCycleEvents(8L)).thenReturn(List.of());
        when(client.accessToken(any())).thenReturn("access-token");
        when(client.insertOrUpdate(eq("primary"), anyString(), eq("access-token"), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        service.sync(new CycleGoogleSyncRequestedEvent(7L));

        verify(connectionMapper).upsertCycleEvent(eq(7L), eq(7L), anyString(), anyString(), anyString());
        verify(connectionMapper).upsertCycleEvent(eq(8L), eq(7L), anyString(), anyString(), anyString());
    }

    @Test
    void 너무_빠르다는_응답과_일시_오류만_다시_시도한다() {
        assertThat(GoogleCalendarClient.isRetryable(429, "")).isTrue();
        assertThat(GoogleCalendarClient.isRetryable(503, "")).isTrue();
        assertThat(GoogleCalendarClient.isRetryable(403, "{\"reason\": \"rateLimitExceeded\"}")).isTrue();
        assertThat(GoogleCalendarClient.isRetryable(403, "{\"reason\": \"forbidden\"}")).isFalse();
        assertThat(GoogleCalendarClient.isRetryable(400, "")).isFalse();
    }

    private GoogleCalendarConnection connection(long userId) {
        return new GoogleCalendarConnection(userId, "encrypted-refresh-token", "primary");
    }
}
