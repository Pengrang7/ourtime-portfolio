-- 초대만 받고 아직 접속하지 않은 사용자는 닉네임이 비어 있다. 첫 접속 때 본인이 입력한 닉네임으로 채운다.
ALTER TABLE app_user
    MODIFY COLUMN nickname VARCHAR(100) NULL;

-- 태그마다 사용자가 고른 아이콘(이모지) 하나를 저장한다.
ALTER TABLE tag
    ADD COLUMN icon VARCHAR(16) NULL AFTER name;
