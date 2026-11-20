package com.placeboard.board;

import com.placeboard.cycle.BoardCyclePeriodsChangedEvent;
import com.placeboard.place.BoardPlacesChangedEvent;
import com.placeboard.schedule.BoardSchedulesChangedEvent;
import com.placeboard.tag.BoardTagsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BoardEventStream {
    private static final Logger log = LoggerFactory.getLogger(BoardEventStream.class);
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final ConcurrentHashMap<Long, Set<Subscription>> subscriptions = new ConcurrentHashMap<>();

    public SseEmitter subscribe(long boardId, long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        Subscription subscription = new Subscription(userId, emitter);
        subscriptions.computeIfAbsent(boardId, ignored -> ConcurrentHashMap.newKeySet()).add(subscription);

        Runnable remove = () -> remove(boardId, subscription);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ignored -> remove.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data(boardId));
        } catch (IOException exception) {
            remove.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishPlacesChanged(BoardPlacesChangedEvent event) {
        publish(event.boardId(), event.changedByUserId(), "places-changed");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishSchedulesChanged(BoardSchedulesChangedEvent event) {
        publish(event.boardId(), event.changedByUserId(), "schedules-changed");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishCyclePeriodsChanged(BoardCyclePeriodsChangedEvent event) {
        publish(event.boardId(), event.changedByUserId(), "cycle-periods-changed");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishTagsChanged(BoardTagsChangedEvent event) {
        publish(event.boardId(), event.changedByUserId(), "tags-changed");
    }

    private void publish(long boardId, long changedByUserId, String eventName) {
        Set<Subscription> boardSubscriptions = subscriptions.get(boardId);
        if (boardSubscriptions == null) return;

        for (Subscription subscription : boardSubscriptions) {
            if (subscription.userId() == changedByUserId) continue;
            try {
                subscription.emitter().send(SseEmitter.event()
                        .name(eventName)
                        .data(boardId));
            } catch (IOException | IllegalStateException exception) {
                remove(boardId, subscription);
                log.debug("종료된 보드 이벤트 연결을 제거했습니다. boardId={}, userId={}",
                        boardId, subscription.userId());
            }
        }
    }

    private void remove(long boardId, Subscription subscription) {
        subscriptions.computeIfPresent(boardId, (ignored, boardSubscriptions) -> {
            boardSubscriptions.remove(subscription);
            return boardSubscriptions.isEmpty() ? null : boardSubscriptions;
        });
    }

    private record Subscription(long userId, SseEmitter emitter) {
    }
}
