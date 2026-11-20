CREATE TABLE schedule_tag (
    schedule_tag_id BIGINT NOT NULL AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (schedule_tag_id),
    UNIQUE KEY uk_schedule_tag (schedule_id, tag_id),
    CONSTRAINT fk_schedule_tag_schedule FOREIGN KEY (schedule_id) REFERENCES board_schedule (schedule_id),
    CONSTRAINT fk_schedule_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (tag_id),
    CONSTRAINT fk_schedule_tag_created_by FOREIGN KEY (created_by) REFERENCES app_user (user_id),
    KEY idx_schedule_tag_tag_use (tag_id, use_yn)
);
