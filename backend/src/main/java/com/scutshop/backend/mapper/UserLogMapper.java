package com.scutshop.backend.mapper;

import com.scutshop.backend.model.UserLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserLogMapper {
    @Insert("INSERT INTO user_log (user_id, action, details) VALUES (#{userId}, #{action}, #{details})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserLog log);

    @Select("SELECT l.*, u.username FROM user_log l JOIN user u ON l.user_id = u.id ORDER BY l.created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<UserLog> selectAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT l.*, u.username FROM user_log l JOIN user u ON l.user_id = u.id WHERE l.user_id = #{userId} ORDER BY l.created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<UserLog> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(1) FROM user_log")
    int countAll();

    @Select("SELECT COUNT(1) FROM user_log WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);
}
