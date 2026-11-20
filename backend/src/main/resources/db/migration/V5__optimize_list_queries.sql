-- 상태 필터 유무에 따라 장소 목록의 정렬까지 인덱스로 처리한다.
ALTER TABLE board_place
    DROP INDEX idx_board_place_filter,
    ADD INDEX idx_board_place_active_updated (board_id, use_yn, updated_at DESC),
    ADD INDEX idx_board_place_active_status_updated (board_id, use_yn, status, updated_at DESC);

-- 장소별 활성 메모 조회와 작성일 정렬을 함께 지원한다.
ALTER TABLE place_memo
    DROP INDEX idx_place_memo_board_place_use,
    ADD INDEX idx_place_memo_active_created (board_place_id, use_yn, created_at);
