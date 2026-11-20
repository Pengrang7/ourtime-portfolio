CREATE TABLE app_user (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    google_sub VARCHAR(255) NULL,
    email VARCHAR(320) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_app_user_google_sub (google_sub),
    UNIQUE KEY uk_app_user_email (email),
    KEY idx_app_user_use_yn (use_yn)
);

CREATE TABLE board (
    board_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    owner_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (board_id),
    CONSTRAINT fk_board_owner FOREIGN KEY (owner_id) REFERENCES app_user (user_id),
    KEY idx_board_owner_use (owner_id, use_yn)
);

CREATE TABLE board_member (
    board_member_id BIGINT NOT NULL AUTO_INCREMENT,
    board_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    can_edit_place BOOLEAN NOT NULL DEFAULT FALSE,
    can_manage_tag BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete_place BOOLEAN NOT NULL DEFAULT FALSE,
    can_invite_member BOOLEAN NOT NULL DEFAULT FALSE,
    can_manage_member BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (board_member_id),
    UNIQUE KEY uk_board_member (board_id, user_id),
    CONSTRAINT fk_board_member_board FOREIGN KEY (board_id) REFERENCES board (board_id),
    CONSTRAINT fk_board_member_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    KEY idx_board_member_user_use (user_id, use_yn)
);

CREATE TABLE place (
    place_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    country VARCHAR(10) NULL,
    sido VARCHAR(100) NULL,
    sigungu VARCHAR(100) NULL,
    dong VARCHAR(100) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (place_id),
    CONSTRAINT fk_place_created_by FOREIGN KEY (created_by) REFERENCES app_user (user_id),
    CONSTRAINT fk_place_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (user_id),
    KEY idx_place_region_use (sido, sigungu, use_yn),
    KEY idx_place_name (name)
);

CREATE TABLE place_source (
    place_source_id BIGINT NOT NULL AUTO_INCREMENT,
    place_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_place_id VARCHAR(255) NULL,
    original_url VARCHAR(2048) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (place_source_id),
    UNIQUE KEY uk_place_source_provider (provider, provider_place_id),
    CONSTRAINT fk_place_source_place FOREIGN KEY (place_id) REFERENCES place (place_id),
    KEY idx_place_source_place_use (place_id, use_yn)
);

CREATE TABLE board_place (
    board_place_id BIGINT NOT NULL AUTO_INCREMENT,
    board_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WANT',
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (board_place_id),
    UNIQUE KEY uk_board_place (board_id, place_id),
    CONSTRAINT fk_board_place_board FOREIGN KEY (board_id) REFERENCES board (board_id),
    CONSTRAINT fk_board_place_place FOREIGN KEY (place_id) REFERENCES place (place_id),
    CONSTRAINT fk_board_place_created_by FOREIGN KEY (created_by) REFERENCES app_user (user_id),
    KEY idx_board_place_filter (board_id, use_yn, status),
    KEY idx_board_place_place (place_id)
);

CREATE TABLE tag (
    tag_id BIGINT NOT NULL AUTO_INCREMENT,
    board_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (tag_id),
    UNIQUE KEY uk_tag_board_name (board_id, name),
    CONSTRAINT fk_tag_board FOREIGN KEY (board_id) REFERENCES board (board_id),
    CONSTRAINT fk_tag_created_by FOREIGN KEY (created_by) REFERENCES app_user (user_id),
    KEY idx_tag_board_use (board_id, use_yn)
);

CREATE TABLE place_tag (
    place_tag_id BIGINT NOT NULL AUTO_INCREMENT,
    board_place_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (place_tag_id),
    UNIQUE KEY uk_place_tag (board_place_id, tag_id),
    CONSTRAINT fk_place_tag_board_place FOREIGN KEY (board_place_id) REFERENCES board_place (board_place_id),
    CONSTRAINT fk_place_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (tag_id),
    CONSTRAINT fk_place_tag_created_by FOREIGN KEY (created_by) REFERENCES app_user (user_id),
    KEY idx_place_tag_tag_use (tag_id, use_yn)
);

CREATE TABLE place_memo (
    memo_id BIGINT NOT NULL AUTO_INCREMENT,
    board_place_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (memo_id),
    CONSTRAINT fk_place_memo_board_place FOREIGN KEY (board_place_id) REFERENCES board_place (board_place_id),
    CONSTRAINT fk_place_memo_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    KEY idx_place_memo_board_place_use (board_place_id, use_yn),
    KEY idx_place_memo_user_use (user_id, use_yn)
);
