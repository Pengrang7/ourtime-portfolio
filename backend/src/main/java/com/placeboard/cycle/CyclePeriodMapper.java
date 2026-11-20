package com.placeboard.cycle;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CyclePeriodMapper {
    @Select("""
            SELECT period_id, start_date, end_date, created_at, updated_at
            FROM user_cycle_period
            WHERE user_id = #{userId}
            ORDER BY start_date DESC, period_id DESC
            LIMIT 24
            """)
    List<CyclePeriodResponse> findAll(long userId);

    @Select("""
            WITH board_users AS (
                SELECT owner_id AS user_id
                FROM board
                WHERE board_id = #{boardId} AND use_yn = 'Y'
                UNION
                SELECT user_id
                FROM board_member
                WHERE board_id = #{boardId} AND use_yn = 'Y'
            ), ranked_periods AS (
                SELECT p.period_id, p.user_id, p.start_date, p.end_date, p.created_at, p.updated_at,
                       ROW_NUMBER() OVER (PARTITION BY p.user_id ORDER BY p.start_date DESC, p.period_id DESC) AS rn
                FROM user_cycle_period p
                JOIN board_users bu ON bu.user_id = p.user_id
            )
            SELECT p.period_id, p.user_id, COALESCE(u.nickname, '멤버') AS nickname,
                   p.start_date, p.end_date, p.created_at, p.updated_at
            FROM ranked_periods p
            JOIN app_user u ON u.user_id = p.user_id AND u.use_yn = 'Y'
            WHERE p.rn <= 24
            ORDER BY p.user_id, p.start_date DESC, p.period_id DESC
            """)
    List<BoardCyclePeriodResponse> findAllForBoard(long boardId);

    @Select("""
            SELECT board_id
            FROM board
            WHERE owner_id = #{userId} AND use_yn = 'Y'
            UNION
            SELECT bm.board_id
            FROM board_member bm
            JOIN board b ON b.board_id = bm.board_id AND b.use_yn = 'Y'
            WHERE bm.user_id = #{userId} AND bm.use_yn = 'Y'
            """)
    List<Long> findBoardIdsForUser(long userId);

    @Select("""
            SELECT period_id, start_date, end_date, created_at, updated_at
            FROM user_cycle_period
            WHERE period_id = #{periodId} AND user_id = #{userId}
            """)
    CyclePeriodResponse findById(@Param("periodId") long periodId, @Param("userId") long userId);

    @Insert("""
            INSERT INTO user_cycle_period (user_id, start_date, end_date)
            VALUES (#{userId}, #{request.startDate}, #{request.endDate})
            """)
    void insert(@Param("userId") long userId, @Param("request") CyclePeriodRequest request);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();

    @Update("""
            UPDATE user_cycle_period
            SET start_date = #{request.startDate}, end_date = #{request.endDate},
                updated_at = CURRENT_TIMESTAMP(6)
            WHERE period_id = #{periodId} AND user_id = #{userId}
            """)
    int update(@Param("periodId") long periodId, @Param("userId") long userId,
               @Param("request") CyclePeriodRequest request);

    @Delete("DELETE FROM user_cycle_period WHERE period_id = #{periodId} AND user_id = #{userId}")
    int delete(@Param("periodId") long periodId, @Param("userId") long userId);
}
