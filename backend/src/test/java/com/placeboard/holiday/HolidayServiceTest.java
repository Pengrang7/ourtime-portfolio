package com.placeboard.holiday;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class HolidayServiceTest {
    private static final String ICS = String.join("\r\n",
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT",
            "DTSTART;VALUE=DATE:20261003",
            "DTEND;VALUE=DATE:20261004",
            "DESCRIPTION:공휴일",
            "SUMMARY:개천절",
            "END:VEVENT",
            "BEGIN:VEVENT",
            "DTSTART;VALUE=DATE:20261001",
            "DTEND;VALUE=DATE:20261002",
            "DESCRIPTION:기념일\\n기념일을 숨기려면 Google Calendar 설정 > 대한민국의 휴일 캘",
            " 린더로 이동하세요.",
            "SUMMARY:국군의날",
            "END:VEVENT",
            "BEGIN:VEVENT",
            "DTSTART;VALUE=DATE:20261005",
            "DTEND;VALUE=DATE:20261006",
            "DESCRIPTION:공휴일",
            "SUMMARY:쉬는 날 개천절",
            "END:VEVENT",
            "END:VCALENDAR");

    @Test
    void 기념일은_빼고_대체공휴일_이름을_바꾼다() {
        assertThat(HolidayIcsParser.parse(ICS)).containsExactly(
                new HolidayResponse(LocalDate.of(2026, 10, 3), "개천절"),
                new HolidayResponse(LocalDate.of(2026, 10, 5), "대체공휴일(개천절)"));
    }

    @Test
    void 한_달에_한_번만_받아_오고_실패하면_이전_목록을_쓴다() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(Instant.parse("2026-10-06T00:00:00Z"));
        HolidayService service = new HolidayService(() -> {
            if (calls.incrementAndGet() > 1) throw new IllegalStateException("network");
            return ICS;
        }, clock);

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 11, 1);
        assertThat(service.find(from, to)).hasSize(2);
        clock.now = clock.now.plusSeconds(29L * 24 * 3600);
        assertThat(service.find(from, to)).hasSize(2);
        assertThat(calls).hasValue(1);

        clock.now = clock.now.plusSeconds(2L * 24 * 3600);
        assertThat(service.find(from, to)).hasSize(2);
        assertThat(calls).hasValue(2);
        assertThat(service.find(LocalDate.of(2026, 10, 4), to)).extracting(HolidayResponse::name)
                .containsExactly("대체공휴일(개천절)");
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
