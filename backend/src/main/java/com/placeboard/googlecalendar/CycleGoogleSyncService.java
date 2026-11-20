package com.placeboard.googlecalendar;

import com.placeboard.cycle.CycleCalendarEntry;
import com.placeboard.cycle.CycleCalendarPlanner;
import com.placeboard.cycle.CyclePeriodMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자의 생리 기록과 앱이 계산한 예정 구간을 Google Calendar 종일 일정으로 맞춘다.
 * 구간마다 (종류, 시작일, 종료일) 키를 두고, 기록이 바뀌면 사라진 키는 지우고 남은 키는 덮어쓰고 새 키는 추가한다.
 */
@Service
public class CycleGoogleSyncService {
    private static final Logger log = LoggerFactory.getLogger(CycleGoogleSyncService.class);
    /** 앞으로 몇 번의 생리 예정 주기까지 캘린더에 미리 올릴지. */
    static final int FUTURE_CYCLES = 6;

    private final CyclePeriodMapper cyclePeriodMapper;
    private final GoogleCalendarConnectionMapper connectionMapper;
    private final GoogleCalendarClient client;
    private final Map<Long, Object> userLocks = new ConcurrentHashMap<>();

    public CycleGoogleSyncService(
            CyclePeriodMapper cyclePeriodMapper,
            GoogleCalendarConnectionMapper connectionMapper,
            GoogleCalendarClient client) {
        this.cyclePeriodMapper = cyclePeriodMapper;
        this.connectionMapper = connectionMapper;
        this.client = client;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sync(CycleGoogleSyncRequestedEvent event) {
        reconcileSafely(event.userId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void syncAfterConnected(GoogleCalendarConnectedEvent event) {
        reconcileSafely(event.userId());
    }

    private void reconcileSafely(long userId) {
        try {
            // 연속 수정으로 동시에 여러 번 실행되면 같은 구간을 중복 생성할 수 있어 사용자 단위로 직렬화한다.
            synchronized (userLocks.computeIfAbsent(userId, key -> new Object())) {
                reconcile(userId);
            }
        } catch (Exception exception) {
            // 다음 기록 변경 때 전체를 다시 맞추므로 여기서는 로그만 남긴다.
            log.error("Google Calendar 생리 주기 동기화 실패: userId={}", userId, exception);
        }
    }

    private void reconcile(long userId) {
        GoogleCalendarConnection connection = connectionMapper.findConnection(userId);
        if (connection == null) return;

        Map<String, CycleCalendarEntry> desired = new LinkedHashMap<>();
        for (CycleCalendarEntry entry : CycleCalendarPlanner.plan(cyclePeriodMapper.findAll(userId), FUTURE_CYCLES)) {
            desired.put(eventKey(entry), entry);
        }
        List<CycleGoogleEvent> existing = connectionMapper.findCycleEvents(userId);
        if (desired.isEmpty() && existing.isEmpty()) return;

        String accessToken = client.accessToken(connection);
        for (CycleGoogleEvent event : existing) {
            CycleCalendarEntry kept = desired.remove(event.eventKey());
            try {
                if (kept != null) {
                    // 이미 올라간 구간은 제목·색 같은 표시 형식이 바뀐 경우에만 덮어써서 Google 호출 수를 줄인다.
                    CycleEventRequest request = request(event.googleEventId(), kept);
                    String hash = contentHash(request);
                    if (hash.equals(event.contentHash())) continue;
                    client.update(connection.calendarId(), event.googleEventId(), accessToken, request);
                    connectionMapper.upsertCycleEvent(userId, event.eventKey(), event.googleEventId(), hash);
                } else {
                    client.delete(connection.calendarId(), event.googleEventId(), accessToken);
                    connectionMapper.deleteCycleEvent(userId, event.eventKey());
                }
            } catch (Exception exception) {
                log.error("Google Calendar 생리 주기 일정 {} 실패: userId={}, key={}",
                        kept != null ? "수정" : "삭제", userId, event.eventKey(), exception);
            }
        }
        for (Map.Entry<String, CycleCalendarEntry> entry : desired.entrySet()) {
            try {
                String eventId = GoogleCalendarClient.deterministicEventId(
                        "pc", "placeboard-cycle:" + userId + ":" + entry.getKey());
                CycleEventRequest request = request(eventId, entry.getValue());
                eventId = client.insertOrUpdate(connection.calendarId(), eventId, accessToken, request);
                connectionMapper.upsertCycleEvent(userId, entry.getKey(), eventId, contentHash(request));
            } catch (Exception exception) {
                log.error("Google Calendar 생리 주기 일정 생성 실패: userId={}, key={}",
                        userId, entry.getKey(), exception);
            }
        }
    }

    /** 이벤트 ID 를 뺀 보낼 내용의 지문. 같으면 Google 에 다시 보내지 않는다. */
    static String contentHash(CycleEventRequest request) {
        return GoogleCalendarClient.deterministicEventId("", request.withId(null).toString());
    }

    static String eventKey(CycleCalendarEntry entry) {
        return entry.kind().name() + ":" + entry.startDate() + ":" + entry.endDate();
    }

    static CycleEventRequest request(String eventId, CycleCalendarEntry entry) {
        // 종일 일정의 end.date 는 마지막 날의 다음 날(배타적)이다.
        return new CycleEventRequest(eventId, summary(entry.kind()),
                new GoogleDate(entry.startDate().toString()),
                new GoogleDate(entry.endDate().plusDays(1).toString()),
                "private", "transparent", "confirmed", new Reminders(false, List.of()), colorId(entry.kind()));
    }

    /** Google Calendar colorId: 생리·생리 예정 = 라벤더(1), 가임기 = 세이지(2), 배란 예정 = 바나나(5). */
    static String colorId(CycleCalendarEntry.Kind kind) {
        return switch (kind) {
            case PERIOD, PREDICTED_PERIOD -> "1";
            case FERTILE -> "2";
            case OVULATION -> "5";
        };
    }

    /** 캘린더를 다른 사람과 공유하는 경우를 고려해 직접적인 단어 대신 은유적인 제목을 쓴다. */
    static String summary(CycleCalendarEntry.Kind kind) {
        return switch (kind) {
            case PERIOD -> "🌸";
            case PREDICTED_PERIOD -> "🌸 예정";
            case FERTILE -> "🌱";
            case OVULATION -> "🌼";
        };
    }

    record GoogleDate(String date) {
    }

    record Reminders(boolean useDefault, List<Object> overrides) {
    }

    record CycleEventRequest(
            String id,
            String summary,
            GoogleDate start,
            GoogleDate end,
            String visibility,
            String transparency,
            String status,
            Reminders reminders,
            String colorId
    ) {
        CycleEventRequest withId(String eventId) {
            return new CycleEventRequest(eventId, summary, start, end, visibility, transparency, status, reminders, colorId);
        }
    }
}
