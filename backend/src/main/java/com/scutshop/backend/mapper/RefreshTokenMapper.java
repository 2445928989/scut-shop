package com.scutshop.backend.mapper;

import com.scutshop.backend.model.RefreshToken;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RefreshTokenMapper {
    @Insert("INSERT INTO `refresh_token` (token, user_id, expires_at, revoked) VALUES (#{token}, #{userId}, #{expiresAt}, #{revoked})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RefreshToken rt);

    @Select("SELECT * FROM `refresh_token` WHERE token = #{token} LIMIT 1")
    RefreshToken selectByToken(@Param("token") String token);

    @Update("UPDATE `refresh_token` SET revoked = 1 WHERE token = #{token}")
    int revokeByToken(@Param("token") String token);

    @Delete("DELETE FROM `refresh_token` WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM `refresh_token` WHERE user_id = #{userId}")
    List<RefreshToken> findByUserId(@Param("userId") Long userId);
}
