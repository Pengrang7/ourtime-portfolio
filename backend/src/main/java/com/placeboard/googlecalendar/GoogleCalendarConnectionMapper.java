package com.placeboard.googlecalendar;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GoogleCalendarConnectionMapper {
    @Select("""
            SELECT user_id, encrypted_refresh_token, calendar_id
            FROM google_calendar_connection
            WHERE user_id = #{userId} AND use_yn = 'Y'
            """)
    GoogleCalendarConnection findConnection(long userId);

    @Insert("""
            INSERT INTO google_calendar_connection
                (user_id, encrypted_refresh_token, calendar_id, use_yn)
            VALUES (#{userId}, #{encryptedRefreshToken}, 'primary', 'Y')
            ON DUPLICATE KEY UPDATE
                encrypted_refresh_token = VALUES(encrypted_refresh_token),
                calendar_id = 'primary', use_yn = 'Y', updated_at = CURRENT_TIMESTAMP(6)
            """)
    void upsertConnection(@Param("userId") long userId,
                          @Param("encryptedRefreshToken") String encryptedRefreshToken);

    @Update("""
            UPDATE google_calendar_connection
            SET use_yn = 'N', updated_at = CURRENT_TIMESTAMP(6)
            WHERE user_id = #{userId} AND use_yn = 'Y'
            """)
    int disableConnection(long userId);

    @Select("""
            SELECT b.owner_id
            FROM board_schedule s
            JOIN board b ON b.board_id = s.board_id AND b.use_yn = 'Y'
            JOIN google_calendar_connection gcc ON gcc.user_id = b.owner_id AND gcc.use_yn = 'Y'
            WHERE s.schedule_id = #{scheduleId}
            UNION
            SELECT bm.user_id
            FROM board_schedule s
            JOIN board b ON b.board_id = s.board_id AND b.use_yn = 'Y'
            JOIN board_member bm ON bm.board_id = b.board_id AND bm.use_yn = 'Y'
            JOIN google_calendar_connection gcc ON gcc.user_id = bm.user_id AND gcc.use_yn = 'Y'
            WHERE s.schedule_id = #{scheduleId}
            ORDER BY 1
            """)
    List<Long> findConnectedUserIdsForSchedule(long scheduleId);

    @Select("""
            SELECT user_id
            FROM schedule_google_event
            WHERE schedule_id = #{scheduleId}
            ORDER BY user_id
            """)
    List<Long> findSyncedUserIds(long scheduleId);

    @Select("""
            SELECT sge.schedule_id
            FROM schedule_google_event sge
            JOIN board_schedule s ON s.schedule_id = sge.schedule_id
            JOIN board b ON b.board_id = s.board_id
            LEFT JOIN board_member bm ON bm.board_id = b.board_id
                AND bm.user_id = #{userId} AND bm.use_yn = 'Y'
            WHERE sge.user_id = #{userId}
              AND (s.use_yn <> 'Y' OR b.use_yn <> 'Y'
                   OR (b.owner_id <> #{userId} AND bm.board_member_id IS NULL))
            ORDER BY sge.schedule_id
            """)
    List<Long> findStaleScheduleIds(long userId);

    @Select("""
            SELECT google_event_id
            FROM schedule_google_event
            WHERE schedule_id = #{scheduleId} AND user_id = #{userId}
            """)
    String findEventId(@Param("scheduleId") long scheduleId, @Param("userId") long userId);

    @Insert("""
            INSERT INTO schedule_google_event (schedule_id, user_id, google_event_id)
            VALUES (#{scheduleId}, #{userId}, #{eventId})
            ON DUPLICATE KEY UPDATE google_event_id = VALUES(google_event_id), synced_at = CURRENT_TIMESTAMP(6)
            """)
    void upsertEvent(@Param("scheduleId") long scheduleId, @Param("userId") long userId,
                     @Param("eventId") String eventId);

    @Update("""
            UPDATE schedule_google_event
            SET synced_at = CURRENT_TIMESTAMP(6)
            WHERE schedule_id = #{scheduleId} AND user_id = #{userId}
            """)
    void touchEvent(@Param("scheduleId") long scheduleId, @Param("userId") long userId);

    @Delete("""
            DELETE FROM schedule_google_event
            WHERE schedule_id = #{scheduleId} AND user_id = #{userId}
            """)
    void deleteEvent(@Param("scheduleId") long scheduleId, @Param("userId") long userId);

    @Select("""
            SELECT event_key, google_event_id, content_hash
            FROM cycle_google_event
            WHERE user_id = #{userId}
            ORDER BY event_key
            """)
    List<CycleGoogleEvent> findCycleEvents(long userId);

    @Insert("""
            INSERT INTO cycle_google_event (user_id, event_key, google_event_id, content_hash)
            VALUES (#{userId}, #{eventKey}, #{eventId}, #{contentHash})
            ON DUPLICATE KEY UPDATE google_event_id = VALUES(google_event_id), content_hash = VALUES(content_hash),
                synced_at = CURRENT_TIMESTAMP(6)
            """)
    void upsertCycleEvent(@Param("userId") long userId, @Param("eventKey") String eventKey,
                          @Param("eventId") String eventId, @Param("contentHash") String contentHash);

    @Delete("""
            DELETE FROM cycle_google_event
            WHERE user_id = #{userId} AND event_key = #{eventKey}
            """)
    void deleteCycleEvent(@Param("userId") long userId, @Param("eventKey") String eventKey);
}
