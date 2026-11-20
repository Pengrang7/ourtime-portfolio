package com.placeboard.review;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PlaceReviewMapper {
    @Select("""
            SELECT COUNT(*) > 0 FROM board_place
            WHERE board_id = #{boardId} AND board_place_id = #{boardPlaceId} AND use_yn = 'Y'
            """)
    boolean placeExists(@Param("boardId") long boardId, @Param("boardPlaceId") long boardPlaceId);

    @Select("""
            SELECT pr.review_id, pr.board_place_id, pr.user_id, u.nickname,
                   pr.revisit_intention, pr.content, pr.visited_on, pr.created_at, pr.updated_at
            FROM place_review pr
            JOIN board_place bp ON bp.board_place_id = pr.board_place_id AND bp.use_yn = 'Y'
            JOIN app_user u ON u.user_id = pr.user_id
            WHERE bp.board_id = #{boardId} AND pr.board_place_id = #{boardPlaceId} AND pr.use_yn = 'Y'
            ORDER BY pr.updated_at DESC
            """)
    List<PlaceReviewResponse> findAll(@Param("boardId") long boardId,
                                      @Param("boardPlaceId") long boardPlaceId);

    @Select("""
            SELECT pr.review_id, pr.board_place_id, pr.user_id, u.nickname,
                   pr.revisit_intention, pr.content, pr.visited_on, pr.created_at, pr.updated_at
            FROM place_review pr
            JOIN board_place bp ON bp.board_place_id = pr.board_place_id AND bp.use_yn = 'Y'
            JOIN app_user u ON u.user_id = pr.user_id
            WHERE bp.board_id = #{boardId} AND pr.board_place_id = #{boardPlaceId}
              AND pr.user_id = #{userId} AND pr.use_yn = 'Y'
            """)
    PlaceReviewResponse findMine(@Param("boardId") long boardId,
                                 @Param("boardPlaceId") long boardPlaceId,
                                 @Param("userId") long userId);

    @Insert("""
            INSERT INTO place_review (board_place_id, user_id, revisit_intention, content, visited_on, use_yn)
            SELECT bp.board_place_id, #{userId}, #{revisitIntention}, #{content}, #{visitedOn}, 'Y'
            FROM board_place bp
            WHERE bp.board_id = #{boardId} AND bp.board_place_id = #{boardPlaceId} AND bp.use_yn = 'Y'
            ON DUPLICATE KEY UPDATE
                revisit_intention = VALUES(revisit_intention), content = VALUES(content),
                visited_on = VALUES(visited_on), use_yn = 'Y', updated_at = CURRENT_TIMESTAMP(6)
            """)
    void upsert(@Param("boardId") long boardId,
               @Param("boardPlaceId") long boardPlaceId,
               @Param("userId") long userId,
               @Param("revisitIntention") RevisitIntention revisitIntention,
               @Param("content") String content,
               @Param("visitedOn") LocalDate visitedOn);

    @Update("""
            UPDATE place_review pr
            JOIN board_place bp ON bp.board_place_id = pr.board_place_id
            SET pr.use_yn = 'N', pr.updated_at = CURRENT_TIMESTAMP(6)
            WHERE bp.board_id = #{boardId} AND bp.board_place_id = #{boardPlaceId}
              AND bp.use_yn = 'Y' AND pr.user_id = #{userId} AND pr.use_yn = 'Y'
            """)
    int deleteMine(@Param("boardId") long boardId,
                   @Param("boardPlaceId") long boardPlaceId,
                   @Param("userId") long userId);
}
