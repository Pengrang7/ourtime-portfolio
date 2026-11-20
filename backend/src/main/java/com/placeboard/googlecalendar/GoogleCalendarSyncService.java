package com.placeboard.googlecalendar;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placeboard.schedule.ScheduleMapper;
import com.placeboard.schedule.ScheduleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class GoogleCalendarSyncService {
    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarSyncService.class);
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter RFC_3339 = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ScheduleMapper scheduleMapper;
    private final GoogleCalendarConnectionMapper connectionMapper;
    private final GoogleCalendarClient client;

    public GoogleCalendarSyncService(
            ScheduleMapper scheduleMapper,
            GoogleCalendarConnectionMapper connectionMapper,
            GoogleCalendarClient client) {
        this.scheduleMapper = scheduleMapper;
        this.connectionMapper = connectionMapper;
        this.client = client;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sync(ScheduleGoogleSyncRequestedEvent event) {
        if (event.action() == ScheduleGoogleSyncRequestedEvent.Action.DELETE) {
            for (Long userId : connectionMapper.findSyncedUserIds(event.scheduleId())) {
                delete(event.scheduleId(), userId);
            }
            return;
        }

        ScheduleResponse schedule = scheduleMapper.findById(event.scheduleId());
        if (schedule == null) return;
        for (Long userId : connectionMapper.findConnectedUserIdsForSchedule(event.scheduleId())) {
            upsert(schedule, userId);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void syncExistingSchedules(GoogleCalendarConnectedEvent event) {
        // 연결이 끊긴 동안 삭제됐거나 접근 권한이 사라진 일정도 재연결 시 정리한다.
        for (Long scheduleId : connectionMapper.findStaleScheduleIds(event.userId())) {
            delete(scheduleId, event.userId());
        }
        for (Long scheduleId : scheduleMapper.findActiveIdsAccessibleByUser(event.userId())) {
            try {
                ScheduleResponse schedule = scheduleMapper.findById(scheduleId);
                if (schedule != null) upsertForUser(schedule, event.userId());
            } catch (Exception exception) {
                log.error("Google Calendar 기존 일정 동기화 실패: scheduleId={}, userId={}",
                        scheduleId, event.userId(), exception);
            }
        }
    }

    private void upsert(ScheduleResponse schedule, long userId) {
        try {
            upsertForUser(schedule, userId);
        } catch (Exception exception) {
            // 한 사용자의 토큰/API 오류가 다른 공유 멤버의 동기화를 막지 않게 사용자별로 처리한다.
            log.error("Google Calendar 일정 동기화 실패: scheduleId={}, userId={}, action=UPSERT",
                    schedule.scheduleId(), userId, exception);
        }
    }

    private void upsertForUser(ScheduleResponse schedule, long userId) {
        GoogleCalendarConnection connection = connectionMapper.findConnection(userId);
        if (connection == null) return;

        String accessToken = client.accessToken(connection);
        String eventId = connectionMapper.findEventId(schedule.scheduleId(), userId);
        GoogleEventRequest request = request(schedule);
        if (eventId == null) {
            eventId = GoogleCalendarClient.deterministicEventId(
                    "pb", "placeboard:" + userId + ":" + schedule.scheduleId());
            eventId = client.insertOrUpdate(connection.calendarId(), eventId, accessToken, request.withId(eventId));
            connectionMapper.upsertEvent(schedule.scheduleId(), userId, eventId);
            return;
        }
        client.update(connection.calendarId(), eventId, accessToken, request);
        connectionMapper.touchEvent(schedule.scheduleId(), userId);
    }

    private void delete(long scheduleId, long userId) {
        try {
            String eventId = connectionMapper.findEventId(scheduleId, userId);
            GoogleCalendarConnection connection = connectionMapper.findConnection(userId);
            if (eventId == null || connection == null) return;
            client.delete(connection.calendarId(), eventId, client.accessToken(connection));
            connectionMapper.deleteEvent(scheduleId, userId);
        } catch (Exception exception) {
            log.error("Google Calendar 일정 동기화 실패: scheduleId={}, userId={}, action=DELETE",
                    scheduleId, userId, exception);
        }
    }

    private GoogleEventRequest request(ScheduleResponse schedule) {
        String title = firstNonBlank(schedule.title(), schedule.placeName(), "개인 일정");
        String location = joinLocation(schedule.placeName(), schedule.placeAddress());
        if (schedule.allDay()) {
            // 앱과 Google 모두 종일 일정의 종료일을 배타적으로 다루므로 저장된 종료 날짜를 그대로 보낸다.
            return new GoogleEventRequest(null, title, schedule.memo(), location,
                    GoogleDateTime.date(schedule.startAt().toLocalDate().toString()),
                    GoogleDateTime.date(schedule.endAt().toLocalDate().toString()),
                    googleColorId(schedule.color()));
        }
        return new GoogleEventRequest(null, title, schedule.memo(), location,
                GoogleDateTime.dateTime(schedule.startAt().atZone(APP_ZONE).format(RFC_3339), APP_ZONE.getId()),
                GoogleDateTime.dateTime(schedule.endAt().atZone(APP_ZONE).format(RFC_3339), APP_ZONE.getId()),
                googleColorId(schedule.color()));
    }

    /**
     * 앱의 일정 색(SCHEDULE_COLORS)은 Google Calendar 일정 색 11가지와 같으므로 해당 colorId 로 바꾼다.
     * 모르는 색은 캘린더 기본색을 쓴다.
     */
    static String googleColorId(String color) {
        if (color == null) return null;
        return switch (color.toUpperCase()) {
            case "#7986CB" -> "1";  // 라벤더
            case "#33B679" -> "2";  // 세이지
            case "#8E24AA" -> "3";  // 포도
            case "#E67C73" -> "4";  // 플라밍고
            case "#F6BF26" -> "5";  // 바나나
            case "#F4511E" -> "6";  // 귤
            case "#039BE5" -> "7";  // 공작
            case "#616161" -> "8";  // 흑연
            case "#3F51B5" -> "9";  // 블루베리
            case "#0B8043" -> "10"; // 바질
            case "#D50000" -> "11"; // 토마토
            default -> null;
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "개인 일정";
    }

    private String joinLocation(String name, String address) {
        if (name == null || name.isBlank()) return address;
        if (address == null || address.isBlank()) return name;
        return name + ", " + address;
    }

    /** 종일 일정은 date, 시간 일정은 dateTime 만 보내야 하므로 null 필드는 직렬화하지 않는다. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GoogleDateTime(String date, String dateTime, String timeZone) {
        static GoogleDateTime date(String date) {
            return new GoogleDateTime(date, null, null);
        }

        static GoogleDateTime dateTime(String dateTime, String timeZone) {
            return new GoogleDateTime(null, dateTime, timeZone);
        }
    }

    private record GoogleEventRequest(
            String id,
            String summary,
            String description,
            String location,
            GoogleDateTime start,
            GoogleDateTime end,
            String colorId
    ) {
        GoogleEventRequest withId(String eventId) {
            return new GoogleEventRequest(eventId, summary, description, location, start, end, colorId);
        }
    }
}
