CREATE TABLE google_calendar_connection (
    user_id BIGINT NOT NULL,
    encrypted_refresh_token VARCHAR(4096) NOT NULL,
    calendar_id VARCHAR(1024) NOT NULL DEFAULT 'primary',
    connected_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (user_id),
    CONSTRAINT fk_google_calendar_connection_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    KEY idx_google_calendar_connection_use (use_yn)
);

CREATE TABLE schedule_google_event (
    schedule_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    google_event_id VARCHAR(1024) NOT NULL,
    synced_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (schedule_id, user_id),
    CONSTRAINT fk_schedule_google_event_schedule FOREIGN KEY (schedule_id) REFERENCES board_schedule (schedule_id),
    CONSTRAINT fk_schedule_google_event_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
);
