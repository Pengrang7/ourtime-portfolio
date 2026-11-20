package com.placeboard.auth;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Select("""
            SELECT user_id, email, nickname
            FROM app_user
            WHERE email = #{email} AND use_yn = 'Y'
            """)
    UserSummary findByEmail(String email);

    @Select("SELECT user_id, email, nickname FROM app_user WHERE user_id = #{userId} AND use_yn = 'Y'")
    UserSummary findById(long userId);

    @Select("SELECT user_id, email, nickname FROM app_user WHERE google_sub = #{googleSub} AND use_yn = 'Y'")
    UserSummary findByGoogleSub(String googleSub);

    @Insert("INSERT INTO app_user (email, nickname, google_sub) VALUES (#{email}, NULL, #{googleSub})")
    void insertGoogleUser(@Param("email") String email, @Param("googleSub") String googleSub);

    // 이메일로 초대됐거나 로컬로 만든 사용자에 구글 계정을 붙인다. 이미 다른 구글 계정이 붙어 있으면 0건.
    @Update("""
            UPDATE app_user SET google_sub = #{googleSub}
            WHERE user_id = #{userId} AND (google_sub IS NULL OR google_sub = #{googleSub})
            """)
    int linkGoogleSub(@Param("userId") long userId, @Param("googleSub") String googleSub);

    @Insert("""
            INSERT INTO app_user (email, nickname)
            VALUES (#{email}, #{nickname})
            ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), use_yn = 'Y'
            """)
    void upsertLocalUser(String email, String nickname);

    @Insert("""
            INSERT INTO app_user (email, nickname)
            VALUES (#{email}, NULL)
            ON DUPLICATE KEY UPDATE use_yn = 'Y'
            """)
    void insertPendingUser(String email);

    @Update("UPDATE app_user SET nickname = #{nickname} WHERE user_id = #{userId}")
    void updateNickname(@Param("userId") long userId, @Param("nickname") String nickname);
}
