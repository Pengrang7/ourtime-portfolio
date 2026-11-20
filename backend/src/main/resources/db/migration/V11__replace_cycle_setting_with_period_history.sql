CREATE TABLE user_cycle_period (
    period_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (period_id),
    CONSTRAINT fk_user_cycle_period_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT uq_user_cycle_period_start UNIQUE (user_id, start_date),
    CONSTRAINT chk_user_cycle_period_range CHECK (end_date >= start_date),
    KEY idx_user_cycle_period_recent (user_id, start_date DESC)
);

INSERT INTO user_cycle_period (user_id, start_date, end_date, created_at, updated_at)
SELECT user_id,
       last_period_start,
       DATE_ADD(last_period_start, INTERVAL (period_length - 1) DAY),
       created_at,
       updated_at
FROM user_cycle_setting;

DROP TABLE user_cycle_setting;
