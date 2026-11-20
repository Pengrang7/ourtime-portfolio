package com.placeboard.googlecalendar;

import com.placeboard.cycle.CycleCalendarEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CycleGoogleSyncServiceTest {
    @Test
    void 종일_비공개_일정으로_만들고_종료일은_다음날로_보낸다() {
        CycleCalendarEntry entry = new CycleCalendarEntry(CycleCalendarEntry.Kind.PREDICTED_PERIOD,
                LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 13));

        CycleGoogleSyncService.CycleEventRequest request = CycleGoogleSyncService.request("pcabc", entry);

        assertThat(request.start().date()).isEqualTo("2026-10-07");
        assertThat(request.end().date()).isEqualTo("2026-10-14");
        assertThat(request.visibility()).isEqualTo("private");
        assertThat(request.transparency()).isEqualTo("transparent");
        assertThat(request.summary()).doesNotContain("생리");
        assertThat(request.colorId()).isEqualTo("1");
        assertThat(CycleGoogleSyncService.colorId(CycleCalendarEntry.Kind.FERTILE)).isEqualTo("2");
        assertThat(CycleGoogleSyncService.colorId(CycleCalendarEntry.Kind.OVULATION)).isEqualTo("5");
        assertThat(CycleGoogleSyncService.eventKey(entry)).isEqualTo("PREDICTED_PERIOD:2026-10-07:2026-10-13");
    }

    @Test
    void 이벤트_ID가_달라도_내용이_같으면_같은_지문이다() {
        CycleCalendarEntry entry = new CycleCalendarEntry(CycleCalendarEntry.Kind.FERTILE,
                LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 18));
        String hash = CycleGoogleSyncService.contentHash(CycleGoogleSyncService.request("pca", entry));

        assertThat(CycleGoogleSyncService.contentHash(CycleGoogleSyncService.request("pcb", entry))).isEqualTo(hash);
        assertThat(hash).hasSize(64);
        assertThat(CycleGoogleSyncService.contentHash(CycleGoogleSyncService.request("pca",
                new CycleCalendarEntry(CycleCalendarEntry.Kind.OVULATION, entry.startDate(), entry.endDate()))))
                .isNotEqualTo(hash);
    }

    @Test
    void 너무_빠르다는_응답과_일시_오류만_다시_시도한다() {
        assertThat(GoogleCalendarClient.isRetryable(429, "")).isTrue();
        assertThat(GoogleCalendarClient.isRetryable(503, "")).isTrue();
        assertThat(GoogleCalendarClient.isRetryable(403, "{\"reason\": \"rateLimitExceeded\"}")).isTrue();
        assertThat(GoogleCalendarClient.isRetryable(403, "{\"reason\": \"forbidden\"}")).isFalse();
        assertThat(GoogleCalendarClient.isRetryable(400, "")).isFalse();
    }
}
