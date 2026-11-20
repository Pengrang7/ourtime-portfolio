package com.placeboard.cycle;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 프론트엔드 cycleCalendar.ts 와 같은 규칙으로 실제 생리 기간과 예정 구간(생리 예정, 가임기, 배란 예정)을 계산한다.
 * 규칙을 바꿀 때는 두 구현을 함께 수정해야 앱 화면과 외부 캘린더 표시가 어긋나지 않는다.
 */
public final class CycleCalendarPlanner {
    private CycleCalendarPlanner() {
    }

    public static List<CycleCalendarEntry> plan(List<CyclePeriodResponse> periods, int futureCycles) {
        if (periods.isEmpty()) return List.of();
        List<CyclePeriodResponse> sorted = periods.stream()
                .sorted(Comparator.comparing(CyclePeriodResponse::startDate))
                .toList();

        List<CycleCalendarEntry> entries = new ArrayList<>();
        for (CyclePeriodResponse period : sorted) {
            entries.add(new CycleCalendarEntry(CycleCalendarEntry.Kind.PERIOD, period.startDate(), period.endDate()));
        }

        // 생리 주기는 종료일이 아니라 연속된 생리 시작일 사이의 일수로 계산한다.
        List<Long> intervals = new ArrayList<>();
        for (int index = 1; index < sorted.size(); index++) {
            long days = ChronoUnit.DAYS.between(sorted.get(index - 1).startDate(), sorted.get(index).startDate());
            if (days >= 15 && days <= 90) intervals.add(days);
        }
        if (intervals.isEmpty()) return entries;

        int cycleLength = (int) Math.round(intervals.stream().mapToLong(Long::longValue).average().orElseThrow());
        int periodLength = (int) Math.round(sorted.stream()
                .mapToLong(period -> ChronoUnit.DAYS.between(period.startDate(), period.endDate()) + 1)
                .average().orElseThrow());
        LocalDate latestStart = sorted.get(sorted.size() - 1).startDate();
        int totalDays = cycleLength * (futureCycles + 1);

        CycleCalendarEntry.Kind runKind = null;
        LocalDate runStart = null;
        for (int offset = 0; offset <= totalDays; offset++) {
            LocalDate day = latestStart.plusDays(offset);
            CycleCalendarEntry.Kind kind = offset == totalDays || isRecorded(sorted, day)
                    ? null : predictedKind(offset, cycleLength, periodLength);
            if (kind == runKind) continue;
            if (runKind != null) entries.add(new CycleCalendarEntry(runKind, runStart, day.minusDays(1)));
            runKind = kind;
            runStart = day;
        }
        return entries;
    }

    private static CycleCalendarEntry.Kind predictedKind(int offset, int cycleLength, int periodLength) {
        int cycleNumber = offset / cycleLength;
        int cycleDay = offset % cycleLength;
        if (cycleNumber > 0 && cycleDay < periodLength) return CycleCalendarEntry.Kind.PREDICTED_PERIOD;
        if (cycleDay >= 12 && cycleDay <= 14) return CycleCalendarEntry.Kind.OVULATION;
        if ((cycleDay >= 7 && cycleDay <= 11) || (cycleDay >= 15 && cycleDay <= 18)) {
            return CycleCalendarEntry.Kind.FERTILE;
        }
        return null;
    }

    private static boolean isRecorded(List<CyclePeriodResponse> periods, LocalDate day) {
        for (CyclePeriodResponse period : periods) {
            if (!day.isBefore(period.startDate()) && !day.isAfter(period.endDate())) return true;
        }
        return false;
    }
}
