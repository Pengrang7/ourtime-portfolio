-- 바뀐 구간만 Google Calendar 에 다시 보내기 위해 마지막으로 보낸 내용의 지문을 저장한다.
-- 기존 행은 NULL 이라 다음 동기화 때 한 번씩 다시 보내진다.
ALTER TABLE cycle_google_event
    ADD COLUMN content_hash VARCHAR(64) NULL AFTER google_event_id;
