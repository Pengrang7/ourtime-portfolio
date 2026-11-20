package com.placeboard.schedule;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ScheduleRequest(
        @Positive(message = "올바른 장소를 선택해 주세요.") Long boardPlaceId,
        @Size(max = 200, message = "일정 내용은 200자 이하로 입력해 주세요.") String title,
        @NotNull(message = "시작 시간을 입력해 주세요.") LocalDateTime startAt,
        @NotNull(message = "종료 시간을 입력해 주세요.") LocalDateTime endAt,
        Boolean allDay,
        @Size(max = 2000, message = "메모는 2,000자 이하로 입력해 주세요.") String memo,
        @Size(max = 20, message = "태그는 최대 20개까지 선택할 수 있습니다.")
        List<@NotNull(message = "올바른 태그를 선택해 주세요.")
             @Positive(message = "올바른 태그를 선택해 주세요.") Long> tagIds,
        @Pattern(regexp = "#(?:7986CB|33B679|8E24AA|E67C73|F6BF26|F4511E|039BE5|616161|3F51B5|0B8043|D50000)",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "지원하지 않는 일정 색상입니다.") String color
) {
    public ScheduleRequest {
        allDay = Boolean.TRUE.equals(allDay);
    }

    @AssertTrue(message = "종료 시간은 시작 시간보다 늦어야 합니다.")
    public boolean isValidTimeRange() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }

    /** 종일 일정은 시작일 0시부터 마지막 날의 다음 날 0시(배타적)까지로 저장한다. */
    @AssertTrue(message = "종일 일정은 날짜 단위로 입력해 주세요.")
    public boolean isValidAllDayRange() {
        if (!allDay || startAt == null || endAt == null) return true;
        return startAt.toLocalTime().equals(LocalTime.MIDNIGHT) && endAt.toLocalTime().equals(LocalTime.MIDNIGHT);
    }
}
