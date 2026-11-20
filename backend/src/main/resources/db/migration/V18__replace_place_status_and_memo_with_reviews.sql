DROP TABLE place_memo;

ALTER TABLE board_place
    DROP INDEX idx_board_place_active_status_updated,
    DROP COLUMN status;

CREATE TABLE place_review (
    review_id BIGINT NOT NULL AUTO_INCREMENT,
    board_place_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    revisit_intention VARCHAR(10) NOT NULL,
    content VARCHAR(1000) NULL,
    visited_on DATE NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (review_id),
    UNIQUE KEY uk_place_review_user (board_place_id, user_id),
    KEY idx_place_review_filter (board_place_id, use_yn, revisit_intention),
    CONSTRAINT fk_place_review_board_place FOREIGN KEY (board_place_id) REFERENCES board_place (board_place_id),
    CONSTRAINT fk_place_review_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT chk_place_review_revisit CHECK (revisit_intention IN ('YES', 'MAYBE', 'NO'))
);
