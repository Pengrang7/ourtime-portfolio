package com.placeboard.place;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.placeboard.review.RevisitFilter;

import java.util.List;

@Mapper
public interface PlaceMapper {
    @Select("""
            SELECT place_id
            FROM place_source
            WHERE provider = #{provider} AND provider_place_id = #{providerPlaceId}
            LIMIT 1
            """)
    Long findPlaceIdBySource(@Param("provider") String provider, @Param("providerPlaceId") String providerPlaceId);

    @Insert("""
            INSERT INTO place (
                name, address, latitude, longitude, country, sido, sigungu, dong,
                category, phone, created_by, updated_by
            ) VALUES (
                #{request.name}, #{request.address}, #{request.latitude}, #{request.longitude},
                #{request.country}, #{request.sido}, #{request.sigungu}, #{request.dong},
                #{request.category}, #{request.phone}, #{userId}, #{userId}
            )
            """)
    void insertPlace(@Param("request") PlaceSaveRequest request, @Param("userId") long userId);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();

    @Insert("""
            INSERT INTO place_source (place_id, provider, provider_place_id, original_url, source_status, last_verified_at)
            VALUES (#{placeId}, #{request.provider}, NULLIF(#{request.providerPlaceId}, ''), #{request.originalUrl},
                    'ACTIVE', CURRENT_TIMESTAMP(6))
            """)
    void insertSource(@Param("placeId") long placeId, @Param("request") PlaceSaveRequest request);

    @Update("UPDATE place SET use_yn = 'Y' WHERE place_id = #{placeId}")
    void reactivatePlace(long placeId);

    @Update("""
            UPDATE place_source
            SET use_yn = 'Y', original_url = #{request.originalUrl}, source_status = 'ACTIVE',
                last_verified_at = CURRENT_TIMESTAMP(6)
            WHERE provider = #{request.provider} AND provider_place_id = NULLIF(#{request.providerPlaceId}, '')
            """)
    void reactivateSource(@Param("request") PlaceSaveRequest request);

    @Update("""
            UPDATE place
            SET name = #{request.name}, address = #{request.address},
                latitude = #{request.latitude}, longitude = #{request.longitude},
                country = #{request.country}, sido = #{request.sido}, sigungu = #{request.sigungu},
                dong = #{request.dong}, category = #{request.category}, phone = #{request.phone},
                updated_by = #{userId}, updated_at = CURRENT_TIMESTAMP(6)
            WHERE place_id = #{placeId}
            """)
    int updatePlaceFromSaveRequest(@Param("placeId") long placeId,
                                   @Param("request") PlaceSaveRequest request,
                                   @Param("userId") long userId);

    @Update("""
            UPDATE place
            SET name = #{preview.name}, address = #{preview.address},
                latitude = #{preview.latitude}, longitude = #{preview.longitude},
                country = #{preview.country}, sido = #{preview.sido}, sigungu = #{preview.sigungu},
                dong = #{preview.dong}, category = #{preview.category}, phone = #{preview.phone},
                updated_by = #{userId}, updated_at = CURRENT_TIMESTAMP(6)
            WHERE place_id = #{placeId}
            """)
    int updatePlaceFromPreview(@Param("placeId") long placeId,
                               @Param("preview") PlacePreviewResponse preview,
                               @Param("userId") long userId);

    @Select("""
            SELECT p.place_id, ps.provider, ps.provider_place_id, ps.original_url
            FROM board_place bp
            JOIN place p ON p.place_id = bp.place_id AND p.use_yn = 'Y'
            JOIN place_source ps ON ps.place_id = p.place_id AND ps.use_yn = 'Y'
            WHERE bp.board_id = #{boardId} AND bp.board_place_id = #{boardPlaceId} AND bp.use_yn = 'Y'
            LIMIT 1
            """)
    PlaceRefreshSource findRefreshSource(@Param("boardId") long boardId,
                                         @Param("boardPlaceId") long boardPlaceId);

    @Update("""
            UPDATE place_source
            SET source_status = #{status}, last_verified_at = CURRENT_TIMESTAMP(6)
            WHERE place_id = #{placeId} AND provider = #{provider} AND use_yn = 'Y'
            """)
    int updateSourceStatus(@Param("placeId") long placeId,
                           @Param("provider") String provider,
                           @Param("status") PlaceSourceStatus status);

    @Select("SELECT board_place_id FROM board_place WHERE board_id = #{boardId} AND place_id = #{placeId}")
    Long findBoardPlaceId(@Param("boardId") long boardId, @Param("placeId") long placeId);

    @Insert("""
            INSERT INTO board_place (board_id, place_id, created_by)
            VALUES (#{boardId}, #{placeId}, #{userId})
            """)
    void insertBoardPlace(@Param("boardId") long boardId, @Param("placeId") long placeId,
                          @Param("userId") long userId);

    @Update("""
            UPDATE board_place
            SET use_yn = 'Y', updated_at = CURRENT_TIMESTAMP(6)
            WHERE board_place_id = #{boardPlaceId}
            """)
    void reactivateBoardPlace(long boardPlaceId);

    @Select("""
            <script>
            SELECT bp.board_place_id, p.place_id,
                   COALESCE(bp.custom_name, p.name) AS name,
                   COALESCE(bp.custom_address, p.address) AS address,
                   COALESCE(bp.custom_latitude, p.latitude) AS latitude,
                   COALESCE(bp.custom_longitude, p.longitude) AS longitude,
                   COALESCE(bp.custom_country, p.country) AS country,
                   COALESCE(bp.custom_sido, p.sido) AS sido,
                   COALESCE(bp.custom_sigungu, p.sigungu) AS sigungu,
                   COALESCE(bp.custom_dong, p.dong) AS dong,
                   p.category, p.phone, ps.provider, ps.provider_place_id, ps.original_url,
                   ps.source_status, ps.last_verified_at,
                   (SELECT COUNT(*) FROM place_review pr WHERE pr.board_place_id = bp.board_place_id AND pr.use_yn = 'Y' AND pr.revisit_intention = 'YES') AS revisit_yes_count,
                   (SELECT COUNT(*) FROM place_review pr WHERE pr.board_place_id = bp.board_place_id AND pr.use_yn = 'Y' AND pr.revisit_intention = 'MAYBE') AS revisit_maybe_count,
                   (SELECT COUNT(*) FROM place_review pr WHERE pr.board_place_id = bp.board_place_id AND pr.use_yn = 'Y' AND pr.revisit_intention = 'NO') AS revisit_no_count,
                   bp.created_by, creator.nickname AS created_by_nickname, bp.created_at,
                   (SELECT GROUP_CONCAT(t.name ORDER BY t.name SEPARATOR ',')
                    FROM place_tag pt JOIN tag t ON t.tag_id = pt.tag_id AND t.use_yn = 'Y'
                    WHERE pt.board_place_id = bp.board_place_id AND pt.use_yn = 'Y') AS tag_names
            FROM board_place bp
            JOIN place p ON p.place_id = bp.place_id AND p.use_yn = 'Y'
            JOIN app_user creator ON creator.user_id = bp.created_by
            LEFT JOIN place_source ps ON ps.place_id = p.place_id AND ps.use_yn = 'Y'
            WHERE bp.board_id = #{boardId} AND bp.use_yn = 'Y'
            <if test="revisit != null and revisit.name() != 'UNREVIEWED'">
                AND EXISTS (SELECT 1 FROM place_review fpr WHERE fpr.board_place_id = bp.board_place_id
                    AND fpr.use_yn = 'Y' AND fpr.revisit_intention = #{revisit})
            </if>
            <if test="revisit != null and revisit.name() == 'UNREVIEWED'">
                AND NOT EXISTS (SELECT 1 FROM place_review fpr WHERE fpr.board_place_id = bp.board_place_id AND fpr.use_yn = 'Y')
            </if>
            <if test="sido != null and sido != ''">AND COALESCE(bp.custom_sido, p.sido) = #{sido}</if>
            <if test="sigungu != null and sigungu != ''">AND COALESCE(bp.custom_sigungu, p.sigungu) = #{sigungu}</if>
            <if test="keyword != null and keyword != ''">AND COALESCE(bp.custom_name, p.name) LIKE CONCAT('%', #{keyword}, '%')</if>
            <if test="tagId != null">
                AND EXISTS (
                    SELECT 1 FROM place_tag fpt
                    WHERE fpt.board_place_id = bp.board_place_id
                      AND fpt.tag_id = #{tagId} AND fpt.use_yn = 'Y'
                )
            </if>
            ORDER BY bp.updated_at DESC
            </script>
            """)
    List<PlaceListItem> findAll(@Param("boardId") long boardId,
                                @Param("revisit") RevisitFilter revisit,
                                @Param("sido") String sido,
                                @Param("sigungu") String sigungu,
                                @Param("tagId") Long tagId,
                                @Param("keyword") String keyword);

    @Select("""
            SELECT DISTINCT COALESCE(bp.custom_sido, p.sido) AS sido,
                            COALESCE(bp.custom_sigungu, p.sigungu) AS sigungu
            FROM board_place bp
            JOIN place p ON p.place_id = bp.place_id AND p.use_yn = 'Y'
            WHERE bp.board_id = #{boardId}
              AND bp.use_yn = 'Y'
              AND COALESCE(bp.custom_sido, p.sido) IS NOT NULL
              AND COALESCE(bp.custom_sido, p.sido) <> ''
            ORDER BY sido, sigungu
            """)
    List<PlaceRegionOption> findRegionOptions(long boardId);

    @Select("""
            SELECT bp.board_place_id, p.place_id,
                   COALESCE(bp.custom_name, p.name) AS name,
                   COALESCE(bp.custom_address, p.address) AS address,
                   COALESCE(bp.custom_latitude, p.latitude) AS latitude,
                   COALESCE(bp.custom_longitude, p.longitude) AS longitude,
                   COALESCE(bp.custom_country, p.country) AS country,
                   COALESCE(bp.custom_sido, p.sido) AS sido,
                   COALESCE(bp.custom_sigungu, p.sigungu) AS sigungu,
                   COALESCE(bp.custom_dong, p.dong) AS dong,
                   p.category, p.phone, ps.provider, ps.provider_place_id, ps.original_url,
                   ps.source_status, ps.last_verified_at,
                   (SELECT COUNT(*) FROM place_review pr WHERE pr.board_place_id = bp.board_place_id AND pr.use_yn = 'Y' AND pr.revisit_intention = 'YES') AS revisit_yes_count,
                   (SELECT COUNT(*) FROM place_review pr WHERE pr.board_place_id = bp.board_place_id AND pr.use_yn = 'Y' AND pr.revisit_intention = 'MAYBE') AS revisit_maybe_count,
                   (SELECT COUNT(*) FROM place_review pr WHERE pr.board_place_id = bp.board_place_id AND pr.use_yn = 'Y' AND pr.revisit_intention = 'NO') AS revisit_no_count,
                   bp.created_by, creator.nickname AS created_by_nickname, bp.created_at,
                   (SELECT GROUP_CONCAT(t.name ORDER BY t.name SEPARATOR ',')
                    FROM place_tag pt JOIN tag t ON t.tag_id = pt.tag_id AND t.use_yn = 'Y'
                    WHERE pt.board_place_id = bp.board_place_id AND pt.use_yn = 'Y') AS tag_names
            FROM board_place bp
            JOIN place p ON p.place_id = bp.place_id AND p.use_yn = 'Y'
            JOIN app_user creator ON creator.user_id = bp.created_by
            LEFT JOIN place_source ps ON ps.place_id = p.place_id AND ps.use_yn = 'Y'
            WHERE bp.board_id = #{boardId}
              AND bp.board_place_id = #{boardPlaceId}
              AND bp.use_yn = 'Y'
            """)
    PlaceListItem findOne(@Param("boardId") long boardId, @Param("boardPlaceId") long boardPlaceId);

    @Select("""
            SELECT COUNT(*) > 0 FROM board_place
            WHERE board_place_id = #{boardPlaceId} AND board_id = #{boardId} AND use_yn = 'Y'
            """)
    boolean exists(@Param("boardId") long boardId, @Param("boardPlaceId") long boardPlaceId);

    @Update("""
            UPDATE board_place SET use_yn = 'N', updated_at = CURRENT_TIMESTAMP(6)
            WHERE board_place_id = #{boardPlaceId} AND board_id = #{boardId} AND use_yn = 'Y'
            """)
    int softDelete(@Param("boardId") long boardId, @Param("boardPlaceId") long boardPlaceId);

    @Update("""
            UPDATE board_place
            SET custom_name = #{request.name}, custom_address = #{request.address},
                custom_latitude = #{request.latitude}, custom_longitude = #{request.longitude},
                custom_country = #{request.country}, custom_sido = #{request.sido},
                custom_sigungu = #{request.sigungu}, custom_dong = #{request.dong},
                updated_at = CURRENT_TIMESTAMP(6)
            WHERE board_id = #{boardId} AND place_id = #{placeId} AND use_yn = 'Y'
            """)
    int updateBoardPlaceDetails(@Param("boardId") long boardId, @Param("placeId") long placeId,
                                @Param("request") PlaceUpdateRequest request);
}
