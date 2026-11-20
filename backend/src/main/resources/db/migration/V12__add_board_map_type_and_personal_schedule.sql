ALTER TABLE board
    ADD COLUMN map_type VARCHAR(20) NOT NULL DEFAULT 'DOMESTIC' AFTER name;

ALTER TABLE board_schedule
    DROP FOREIGN KEY fk_board_schedule_place;

ALTER TABLE board_schedule
    MODIFY COLUMN board_place_id BIGINT NULL,
    ADD COLUMN title VARCHAR(200) NULL AFTER board_place_id;

ALTER TABLE board_schedule
    ADD CONSTRAINT fk_board_schedule_place
        FOREIGN KEY (board_place_id) REFERENCES board_place (board_place_id);
