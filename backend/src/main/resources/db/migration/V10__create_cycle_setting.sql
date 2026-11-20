CREATE TABLE user_cycle_setting (
    user_id BIGINT NOT NULL,
    last_period_start DATE NOT NULL,
    cycle_length SMALLINT NOT NULL,
    period_length SMALLINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_cycle_setting_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT chk_user_cycle_length CHECK (cycle_length BETWEEN 21 AND 45),
    CONSTRAINT chk_user_period_length CHECK (period_length BETWEEN 1 AND 10)
);
