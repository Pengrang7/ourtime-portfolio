-- 한 사용자의 생리 주기 일정을 공유 보드 구성원 여러 명의 Google Calendar에 동기화한다.
-- user_id는 Google Calendar 소유자, source_user_id는 생리 기록 작성자다.
ALTER TABLE cycle_google_event
    ADD COLUMN source_user_id BIGINT NULL AFTER user_id;

-- 기존 일정은 본인 기록을 본인 캘린더에 동기화한 데이터다.
UPDATE cycle_google_event
SET source_user_id = user_id
WHERE source_user_id IS NULL;

ALTER TABLE cycle_google_event
    MODIFY source_user_id BIGINT NOT NULL,
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (user_id, source_user_id, event_key),
    ADD CONSTRAINT fk_cycle_google_event_source_user
        FOREIGN KEY (source_user_id) REFERENCES app_user (user_id);
