package com.placeboard.cycle;

import java.time.LocalDate;

public record CycleCalendarEntry(Kind kind, LocalDate startDate, LocalDate endDate) {
    public enum Kind { PERIOD, PREDICTED_PERIOD, FERTILE, OVULATION }
}
