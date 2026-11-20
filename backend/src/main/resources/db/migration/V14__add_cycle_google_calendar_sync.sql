CREATE TABLE cycle_google_event (
    user_id BIGINT NOT NULL,
    event_key VARCHAR(64) NOT NULL,
    google_event_id VARCHAR(1024) NOT NULL,
    synced_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, event_key),
    CONSTRAINT fk_cycle_google_event_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
);
