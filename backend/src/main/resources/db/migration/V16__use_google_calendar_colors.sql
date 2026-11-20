-- 일정 색을 Google Calendar 일정 색 11가지로 바꾸면서 기존 색은 가장 가까운 Google 색으로 옮긴다.
UPDATE board_schedule
SET color = CASE UPPER(color)
    WHEN '#168A4A' THEN '#0B8043'
    WHEN '#2563EB' THEN '#3F51B5'
    WHEN '#EA580C' THEN '#F4511E'
    WHEN '#DC2626' THEN '#D50000'
    WHEN '#7C3AED' THEN '#8E24AA'
    WHEN '#DB2777' THEN '#E67C73'
    WHEN '#0F766E' THEN '#039BE5'
    WHEN '#A16207' THEN '#F6BF26'
    ELSE color
END;

ALTER TABLE board_schedule
    ALTER COLUMN color SET DEFAULT '#0B8043';
