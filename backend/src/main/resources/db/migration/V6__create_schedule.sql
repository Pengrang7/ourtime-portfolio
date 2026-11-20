CREATE TABLE board_schedule (
    schedule_id BIGINT NOT NULL AUTO_INCREMENT,
    board_id BIGINT NOT NULL,
    board_place_id BIGINT NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    memo VARCHAR(2000) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (schedule_id),
    CONSTRAINT fk_board_schedule_board FOREIGN KEY (board_id) REFERENCES board (board_id),
    CONSTRAINT fk_board_schedule_place FOREIGN KEY (board_place_id) REFERENCES board_place (board_place_id),
    CONSTRAINT fk_board_schedule_created_by FOREIGN KEY (created_by) REFERENCES app_user (user_id),
    KEY idx_board_schedule_range (board_id, use_yn, start_at),
    KEY idx_board_schedule_place (board_place_id)
);
