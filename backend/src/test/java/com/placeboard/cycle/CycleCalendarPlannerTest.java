package com.placeboard.cycle;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CycleCalendarPlannerTest {
    private static CyclePeriodResponse period(long id, String start, String end) {
        return new CyclePeriodResponse(id, LocalDate.parse(start), LocalDate.parse(end), null, null);
    }

    private static CycleCalendarEntry entry(CycleCalendarEntry.Kind kind, String start, String end) {
        return new CycleCalendarEntry(kind, LocalDate.parse(start), LocalDate.parse(end));
    }

    @Test
    void 실제_기록과_앱이_계산한_예정_구간을_묶어서_만든다() {
        List<CycleCalendarEntry> entries = CycleCalendarPlanner.plan(List.of(
                period(2, "2026-09-07", "2026-09-13"),
                period(1, "2026-08-08", "2026-08-14")), 1);

        assertThat(entries).containsExactly(
                entry(CycleCalendarEntry.Kind.PERIOD, "2026-08-08", "2026-08-14"),
                entry(CycleCalendarEntry.Kind.PERIOD, "2026-09-07", "2026-09-13"),
                entry(CycleCalendarEntry.Kind.FERTILE, "2026-09-14", "2026-09-18"),
                entry(CycleCalendarEntry.Kind.OVULATION, "2026-09-19", "2026-09-21"),
                entry(CycleCalendarEntry.Kind.FERTILE, "2026-09-22", "2026-09-25"),
                entry(CycleCalendarEntry.Kind.PREDICTED_PERIOD, "2026-10-07", "2026-10-13"),
                entry(CycleCalendarEntry.Kind.FERTILE, "2026-10-14", "2026-10-18"),
                entry(CycleCalendarEntry.Kind.OVULATION, "2026-10-19", "2026-10-21"),
                entry(CycleCalendarEntry.Kind.FERTILE, "2026-10-22", "2026-10-25"));
    }

    @Test
    void 기록이_한_번이면_예측_없이_실제_기간만_만든다() {
        List<CycleCalendarEntry> entries = CycleCalendarPlanner.plan(List.of(
                period(1, "2026-08-08", "2026-08-14")), 6);

        assertThat(entries).containsExactly(entry(CycleCalendarEntry.Kind.PERIOD, "2026-08-08", "2026-08-14"));
    }

    @Test
    void 기록이_없으면_아무것도_만들지_않는다() {
        assertThat(CycleCalendarPlanner.plan(List.of(), 6)).isEmpty();
    }
}
