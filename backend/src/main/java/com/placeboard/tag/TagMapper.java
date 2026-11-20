package com.placeboard.tag;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TagMapper {
    @Select("""
            SELECT tag_id, board_id, name, icon FROM tag
            WHERE board_id = #{boardId} AND use_yn = 'Y'
            ORDER BY name
            """)
    List<TagResponse> findAll(long boardId);

    @Insert("""
            INSERT INTO tag (board_id, name, icon, created_by)
            VALUES (#{boardId}, #{name}, #{icon}, #{userId})
            ON DUPLICATE KEY UPDATE icon = VALUES(icon), use_yn = 'Y', updated_at = CURRENT_TIMESTAMP(6)
            """)
    void upsert(@Param("boardId") long boardId, @Param("name") String name, @Param("icon") String icon,
                @Param("userId") long userId);

    @Select("SELECT tag_id, board_id, name, icon FROM tag WHERE board_id = #{boardId} AND name = #{name}")
    TagResponse findByName(@Param("boardId") long boardId, @Param("name") String name);

    @Update("""
            UPDATE tag SET name = #{name}, icon = #{icon}, updated_at = CURRENT_TIMESTAMP(6)
            WHERE tag_id = #{tagId} AND board_id = #{boardId} AND use_yn = 'Y'
            """)
    int update(@Param("boardId") long boardId, @Param("tagId") long tagId,
               @Param("name") String name, @Param("icon") String icon);

    @Update("UPDATE tag SET use_yn = 'N' WHERE tag_id = #{tagId} AND board_id = #{boardId} AND use_yn = 'Y'")
    int softDelete(@Param("boardId") long boardId, @Param("tagId") long tagId);

    @Update("UPDATE place_tag SET use_yn = 'N' WHERE tag_id = #{tagId}")
    void deactivateAssignments(long tagId);

    @Update("UPDATE schedule_tag SET use_yn = 'N' WHERE tag_id = #{tagId}")
    void deactivateScheduleAssignments(long tagId);

    @Update("""
            UPDATE place_tag pt
            JOIN board_place bp ON bp.board_place_id = pt.board_place_id
            SET pt.use_yn = 'N'
            WHERE bp.board_id = #{boardId} AND bp.board_place_id = #{boardPlaceId}
              AND pt.use_yn = 'Y'
            """)
    void clearAssignments(@Param("boardId") long boardId, @Param("boardPlaceId") long boardPlaceId);

    @Insert("""
            INSERT INTO place_tag (board_place_id, tag_id, created_by)
            SELECT bp.board_place_id, t.tag_id, #{userId}
            FROM board_place bp
            JOIN tag t ON t.board_id = bp.board_id AND t.tag_id = #{tagId} AND t.use_yn = 'Y'
            WHERE bp.board_id = #{boardId} AND bp.board_place_id = #{boardPlaceId} AND bp.use_yn = 'Y'
            ON DUPLICATE KEY UPDATE use_yn = 'Y'
            """)
    int assign(@Param("boardId") long boardId, @Param("boardPlaceId") long boardPlaceId,
               @Param("tagId") long tagId, @Param("userId") long userId);

    @Update("""
            UPDATE place_tag pt
            JOIN board_place bp ON bp.board_place_id = pt.board_place_id
            JOIN tag t ON t.tag_id = pt.tag_id AND t.board_id = bp.board_id
            SET pt.use_yn = 'N'
            WHERE bp.board_id = #{boardId} AND pt.board_place_id = #{boardPlaceId}
              AND pt.tag_id = #{tagId} AND pt.use_yn = 'Y'
            """)
    int unassign(@Param("boardId") long boardId, @Param("boardPlaceId") long boardPlaceId,
                 @Param("tagId") long tagId);
}
