ALTER TABLE board_place
    ADD COLUMN custom_name VARCHAR(200) NULL AFTER status,
    ADD COLUMN custom_address VARCHAR(500) NULL AFTER custom_name,
    ADD COLUMN custom_latitude DECIMAL(10, 7) NULL AFTER custom_address,
    ADD COLUMN custom_longitude DECIMAL(10, 7) NULL AFTER custom_latitude,
    ADD COLUMN custom_country VARCHAR(10) NULL AFTER custom_longitude,
    ADD COLUMN custom_sido VARCHAR(100) NULL AFTER custom_country,
    ADD COLUMN custom_sigungu VARCHAR(100) NULL AFTER custom_sido,
    ADD COLUMN custom_dong VARCHAR(100) NULL AFTER custom_sigungu;
