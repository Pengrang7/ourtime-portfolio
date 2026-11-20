package com.placeboard.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Google "대한민국의 휴일" 공개 캘린더(iCal)에서 쉬는 날(공휴일·대체공휴일·임시공휴일)만 뽑는다. */
final class HolidayIcsParser {
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String SUBSTITUTE_PREFIX = "쉬는 날 ";

    private HolidayIcsParser() {
    }

    static List<HolidayResponse> parse(String ics) {
        // iCal 은 긴 줄을 "줄바꿈 + 공백"으로 접어서 보내므로 먼저 이어 붙인다.
        String unfolded = ics.replace("\r\n", "\n").replace("\n ", "").replace("\n\t", "");
        List<HolidayResponse> holidays = new ArrayList<>();
        String start = null;
        String end = null;
        String summary = null;
        String description = null;
        for (String line : unfolded.split("\n")) {
            if (line.equals("BEGIN:VEVENT")) {
                start = end = summary = description = null;
            } else if (line.startsWith("DTSTART")) {
                start = value(line);
            } else if (line.startsWith("DTEND")) {
                end = value(line);
            } else if (line.startsWith("SUMMARY")) {
                summary = value(line).trim();
            } else if (line.startsWith("DESCRIPTION")) {
                description = value(line).trim();
            } else if (line.equals("END:VEVENT")) {
                // 어버이날·국군의날 같은 기념일은 설명이 "기념일"로 되어 있어 쉬는 날에서 뺀다.
                if (start == null || summary == null || description == null || !description.startsWith("공휴일")) continue;
                LocalDate first = LocalDate.parse(start.substring(0, 8), BASIC_DATE);
                LocalDate last = end == null ? first : LocalDate.parse(end.substring(0, 8), BASIC_DATE).minusDays(1);
                for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
                    holidays.add(new HolidayResponse(day, displayName(summary)));
                }
            }
        }
        holidays.sort(Comparator.comparing(HolidayResponse::date));
        return holidays;
    }

    /** Google 은 대체공휴일을 "쉬는 날 개천절"처럼 적으므로 앱에서 쓰는 "대체공휴일(개천절)"로 바꾼다. */
    static String displayName(String summary) {
        if (summary.startsWith(SUBSTITUTE_PREFIX)) {
            return "대체공휴일(" + summary.substring(SUBSTITUTE_PREFIX.length()).trim() + ")";
        }
        return summary;
    }

    private static String value(String line) {
        int colon = line.indexOf(':');
        return colon < 0 ? "" : line.substring(colon + 1);
    }
}
