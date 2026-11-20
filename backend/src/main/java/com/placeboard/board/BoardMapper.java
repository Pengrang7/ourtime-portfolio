package com.placeboard.board;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BoardMapper {
    @Select("""
            SELECT b.board_id, b.name, b.map_type, b.owner_id, owner.nickname AS owner_nickname, b.created_at
            FROM board b
            JOIN app_user owner ON owner.user_id = b.owner_id
            LEFT JOIN board_member bm ON bm.board_id = b.board_id
                AND bm.user_id = #{userId} AND bm.use_yn = 'Y'
            WHERE b.use_yn = 'Y'
              AND (b.owner_id = #{userId} OR bm.board_member_id IS NOT NULL)
            ORDER BY b.created_at DESC
            """)
    List<BoardSummary> findAllAccessible(long userId);

    @Insert("INSERT INTO board (name, map_type, owner_id) VALUES (#{name}, #{mapType}, #{ownerId})")
    void insert(@Param("name") String name, @Param("mapType") BoardMapType mapType,
                @Param("ownerId") long ownerId);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();

    @Select("""
            SELECT b.board_id, b.owner_id, b.map_type,
                   CASE WHEN b.owner_id = #{userId} THEN TRUE ELSE COALESCE(bm.can_edit_place, FALSE) END AS can_edit_place,
                   CASE WHEN b.owner_id = #{userId} THEN TRUE ELSE COALESCE(bm.can_manage_tag, FALSE) END AS can_manage_tag,
                   CASE WHEN b.owner_id = #{userId} THEN TRUE ELSE COALESCE(bm.can_delete_place, FALSE) END AS can_delete_place,
                   CASE WHEN b.owner_id = #{userId} THEN TRUE ELSE COALESCE(bm.can_invite_member, FALSE) END AS can_invite_member
            FROM board b
            LEFT JOIN board_member bm ON bm.board_id = b.board_id
                AND bm.user_id = #{userId} AND bm.use_yn = 'Y'
            WHERE b.board_id = #{boardId} AND b.use_yn = 'Y'
              AND (b.owner_id = #{userId} OR bm.board_member_id IS NOT NULL)
            """)
    BoardAccess findAccess(@Param("boardId") long boardId, @Param("userId") long userId);

    @Insert("""
            INSERT INTO board_member (
                board_id, user_id, can_edit_place, can_manage_tag, can_delete_place, can_invite_member
            ) VALUES (
                #{boardId}, #{userId}, #{request.canEditPlace}, #{request.canManageTag},
                #{request.canDeletePlace}, #{request.canInviteMember}
            )
            ON DUPLICATE KEY UPDATE
                can_edit_place = VALUES(can_edit_place),
                can_manage_tag = VALUES(can_manage_tag),
                can_delete_place = VALUES(can_delete_place),
                can_invite_member = VALUES(can_invite_member),
                use_yn = 'Y'
            """)
    void upsertMember(@Param("boardId") long boardId, @Param("userId") long userId,
                      @Param("request") MemberInviteRequest request);

    @Update("""
            UPDATE board_member
            SET can_edit_place = #{request.canEditPlace},
                can_manage_tag = #{request.canManageTag},
                can_delete_place = #{request.canDeletePlace},
                can_invite_member = #{request.canInviteMember},
                updated_at = CURRENT_TIMESTAMP(6)
            WHERE board_id = #{boardId} AND user_id = #{userId} AND use_yn = 'Y'
            """)
    int updateMemberPermissions(@Param("boardId") long boardId, @Param("userId") long userId,
                                @Param("request") MemberPermissionRequest request);

    @Select("""
            SELECT u.user_id, u.email, u.nickname,
                   (u.user_id = b.owner_id) AS owner,
                   TRUE AS can_edit_place, TRUE AS can_manage_tag, TRUE AS can_delete_place,
                   TRUE AS can_invite_member
            FROM board b
            JOIN app_user u ON u.user_id = b.owner_id
            WHERE b.board_id = #{boardId}
            UNION ALL
            SELECT u.user_id, u.email, u.nickname, FALSE,
                   bm.can_edit_place, bm.can_manage_tag, bm.can_delete_place,
                   bm.can_invite_member
            FROM board_member bm
            JOIN app_user u ON u.user_id = bm.user_id AND u.use_yn = 'Y'
            WHERE bm.board_id = #{boardId} AND bm.use_yn = 'Y'
            ORDER BY owner DESC, nickname IS NULL, nickname, email
            """)
    List<BoardMemberResponse> findMembers(long boardId);

    @Select("""
            SELECT u.user_id, u.email, u.nickname, FALSE AS owner,
                   bm.can_edit_place, bm.can_manage_tag, bm.can_delete_place,
                   bm.can_invite_member
            FROM board_member bm
            JOIN app_user u ON u.user_id = bm.user_id AND u.use_yn = 'Y'
            WHERE bm.board_id = #{boardId} AND bm.user_id = #{userId} AND bm.use_yn = 'Y'
            """)
    BoardMemberResponse findMember(@Param("boardId") long boardId, @Param("userId") long userId);

    @Update("UPDATE board SET name = #{name}, updated_at = CURRENT_TIMESTAMP(6) WHERE board_id = #{boardId} AND use_yn = 'Y'")
    int rename(@Param("boardId") long boardId, @Param("name") String name);

    @Update("UPDATE board SET use_yn = 'N', updated_at = CURRENT_TIMESTAMP(6) WHERE board_id = #{boardId} AND owner_id = #{ownerId} AND use_yn = 'Y'")
    int softDelete(@Param("boardId") long boardId, @Param("ownerId") long ownerId);
}
