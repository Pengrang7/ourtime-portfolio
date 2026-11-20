ALTER TABLE place_source
    ADD COLUMN source_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER original_url,
    ADD COLUMN last_verified_at DATETIME(6) NULL AFTER source_status;

