package com.placeboard.holiday;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * 공휴일은 Google 공개 휴일 캘린더에서 한 달에 한 번만 받아 메모리에 둔다.
 * 받아 오기에 실패하면 이전 목록을 계속 쓰고, 처음부터 실패하면 빈 목록을 돌려 프론트엔드 내장 목록을 쓰게 한다.
 */
@Service
public class HolidayService {
    private static final Logger log = LoggerFactory.getLogger(HolidayService.class);
    private static final Duration REFRESH_INTERVAL = Duration.ofDays(30);
    private static final Duration RETRY_INTERVAL = Duration.ofHours(1);

    private final Supplier<String> icsFetcher;
    private final Clock clock;
    private List<HolidayResponse> cached = List.of();
    private Instant nextRefreshAt = Instant.MIN;

    @Autowired
    public HolidayService(@Value("${placeboard.holiday.ics-url:https://calendar.google.com/calendar/ical/ko.south_korea%23holiday%40group.v.calendar.google.com/public/basic.ics}") String icsUrl) {
        this(() -> RestClient.create().get().uri(URI.create(icsUrl)).retrieve().body(String.class),
                Clock.systemUTC());
    }

    HolidayService(Supplier<String> icsFetcher, Clock clock) {
        this.icsFetcher = icsFetcher;
        this.clock = clock;
    }

    public synchronized List<HolidayResponse> find(LocalDate from, LocalDate to) {
        refreshIfNeeded();
        return cached.stream()
                .filter(holiday -> !holiday.date().isBefore(from) && holiday.date().isBefore(to))
                .toList();
    }

    private void refreshIfNeeded() {
        Instant now = clock.instant();
        if (now.isBefore(nextRefreshAt)) return;
        try {
            String ics = icsFetcher.get();
            List<HolidayResponse> parsed = ics == null ? List.of() : HolidayIcsParser.parse(ics);
            if (parsed.isEmpty()) throw new IllegalStateException("공휴일 목록이 비어 있습니다.");
            cached = parsed;
            nextRefreshAt = now.plus(REFRESH_INTERVAL);
        } catch (Exception exception) {
            log.warn("Google 공휴일 캘린더를 받아 오지 못했습니다. 이전 목록을 사용합니다.", exception);
            nextRefreshAt = now.plus(RETRY_INTERVAL);
        }
    }
}
