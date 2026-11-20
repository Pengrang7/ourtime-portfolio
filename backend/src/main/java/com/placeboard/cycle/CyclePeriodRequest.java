package com.placeboard.cycle;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record CyclePeriodRequest(
        @NotNull(message = "생리 시작일을 입력해 주세요.")
        @PastOrPresent(message = "생리 시작일은 오늘 이후일 수 없습니다.") LocalDate startDate,
        @NotNull(message = "생리 종료일을 입력해 주세요.") LocalDate endDate
) {
    @AssertTrue(message = "종료일은 시작일 이후 14일 이내여야 합니다.")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) return true;
        long duration = ChronoUnit.DAYS.between(startDate, endDate);
        return duration >= 0 && duration < 14;
    }
}
