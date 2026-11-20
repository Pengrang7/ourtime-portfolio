package com.placeboard.schedule;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ScheduleMapper {
    String SELECT_COLUMNS = """
            SELECT s.schedule_id, s.board_id, s.board_place_id, s.title,
                   COALESCE(bp.custom_name, p.name) AS place_name,
                   COALESCE(bp.custom_address, p.address) AS place_address,
                   COALESCE(bp.custom_latitude, p.latitude) AS latitude,
                   COALESCE(bp.custom_longitude, p.longitude) AS longitude,
                   s.start_at, s.end_at, s.all_day, s.memo, s.color,
                   (SELECT GROUP_CONCAT(st.tag_id ORDER BY st.tag_id)
                    FROM schedule_tag st JOIN tag t ON t.tag_id = st.tag_id AND t.use_yn = 'Y'
                    WHERE st.schedule_id = s.schedule_id AND st.use_yn = 'Y') AS tag_ids,
                   (SELECT GROUP_CONCAT(t.name ORDER BY st.tag_id SEPARATOR ', ')
                    FROM schedule_tag st JOIN tag t ON t.tag_id = st.tag_id AND t.use_yn = 'Y'
                    WHERE st.schedule_id = s.schedule_id AND st.use_yn = 'Y') AS tag_names,
                   (SELECT GROUP_CONCAT(NULLIF(t.icon, '') ORDER BY st.tag_id SEPARATOR ' ')
                    FROM schedule_tag st JOIN tag t ON t.tag_id = st.tag_id AND t.use_yn = 'Y'
                    WHERE st.schedule_id = s.schedule_id AND st.use_yn = 'Y') AS tag_icons,
                   s.created_by,
                   u.nickname AS created_by_nickname, s.created_at, s.updated_at
            FROM board_schedule s
            LEFT JOIN board_place bp ON bp.board_place_id = s.board_place_id
            LEFT JOIN place p ON p.place_id = bp.place_id
            JOIN app_user u ON u.user_id = s.created_by
            """;

    @Select(SELECT_COLUMNS + """
            WHERE s.board_id = #{boardId} AND s.use_yn = 'Y'
              AND s.start_at < #{to} AND s.end_at > #{from}
            ORDER BY s.start_at, s.schedule_id
            """)
    List<ScheduleResponse> findAll(@Param("boardId") long boardId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Select(SELECT_COLUMNS + " WHERE s.schedule_id = #{scheduleId} AND s.use_yn = 'Y'")
    ScheduleResponse findById(long scheduleId);

    @Select("""
            SELECT DISTINCT s.schedule_id
            FROM board_schedule s
            JOIN board b ON b.board_id = s.board_id AND b.use_yn = 'Y'
            LEFT JOIN board_member bm ON bm.board_id = b.board_id
                AND bm.user_id = #{userId} AND bm.use_yn = 'Y'
            WHERE s.use_yn = 'Y'
              AND (b.owner_id = #{userId} OR bm.board_member_id IS NOT NULL)
            ORDER BY s.schedule_id
            """)
    List<Long> findActiveIdsAccessibleByUser(long userId);

    @Insert("""
            INSERT INTO board_schedule (board_id, board_place_id, title, start_at, end_at, all_day, memo, color, created_by)
            SELECT #{boardId}, #{request.boardPlaceId}, #{title}, #{request.startAt}, #{request.endAt}, #{request.allDay},
                   #{memo}, #{color}, #{userId}
            FROM board b
            WHERE b.board_id = #{boardId} AND b.use_yn = 'Y'
              AND (#{request.boardPlaceId} IS NULL OR EXISTS (
                  SELECT 1 FROM board_place bp
                  WHERE bp.board_id = #{boardId} AND bp.board_place_id = #{request.boardPlaceId}
                    AND bp.use_yn = 'Y'
              ))
            """)
    int insert(@Param("boardId") long boardId, @Param("request") ScheduleRequest request,
               @Param("title") String title, @Param("memo") String memo,
               @Param("color") String color, @Param("userId") long userId);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();

    @Insert("""
            INSERT INTO schedule_tag (schedule_id, tag_id, created_by)
            SELECT s.schedule_id, t.tag_id, #{userId}
            FROM board_schedule s
            JOIN tag t ON t.board_id = s.board_id AND t.tag_id = #{tagId} AND t.use_yn = 'Y'
            WHERE s.schedule_id = #{scheduleId} AND s.use_yn = 'Y'
            ON DUPLICATE KEY UPDATE use_yn = 'Y', created_by = VALUES(created_by)
            """)
    int assignTag(@Param("scheduleId") long scheduleId, @Param("tagId") long tagId,
                  @Param("userId") long userId);

    @Update("UPDATE schedule_tag SET use_yn = 'N' WHERE schedule_id = #{scheduleId} AND use_yn = 'Y'")
    void clearTags(long scheduleId);

    @Update("""
            UPDATE board_schedule s
            SET s.board_place_id = #{request.boardPlaceId}, s.title = #{title}, s.start_at = #{request.startAt},
                s.end_at = #{request.endAt}, s.all_day = #{request.allDay}, s.memo = #{memo}, s.color = #{color},
                s.updated_at = CURRENT_TIMESTAMP(6)
            WHERE s.schedule_id = #{scheduleId} AND s.board_id = #{boardId} AND s.use_yn = 'Y'
              AND (s.created_by = #{userId} OR #{userId} = #{ownerId})
              AND (#{request.boardPlaceId} IS NULL OR EXISTS (
                  SELECT 1 FROM board_place bp
                  WHERE bp.board_id = s.board_id AND bp.board_place_id = #{request.boardPlaceId}
                    AND bp.use_yn = 'Y'
              ))
            """)
    int update(@Param("boardId") long boardId, @Param("scheduleId") long scheduleId,
               @Param("request") ScheduleRequest request, @Param("title") String title, @Param("memo") String memo,
               @Param("color") String color, @Param("userId") long userId, @Param("ownerId") long ownerId);

    @Update("""
            UPDATE board_schedule
            SET use_yn = 'N', updated_at = CURRENT_TIMESTAMP(6)
            WHERE schedule_id = #{scheduleId} AND board_id = #{boardId} AND use_yn = 'Y'
              AND (created_by = #{userId} OR #{userId} = #{ownerId})
            """)
    int softDelete(@Param("boardId") long boardId, @Param("scheduleId") long scheduleId,
                   @Param("userId") long userId, @Param("ownerId") long ownerId);
}
