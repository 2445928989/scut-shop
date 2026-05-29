package com.scutshop.backend.mapper;

import com.scutshop.backend.model.LoginLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LoginLogMapper {
    @Insert("INSERT INTO `login_log` (user_id, username, ip_address, user_agent) VALUES (#{userId}, #{username}, #{ipAddress}, #{userAgent})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LoginLog log);

    @Select("SELECT * FROM `login_log` WHERE user_id = #{userId} ORDER BY login_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<LoginLog> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM `login_log` ORDER BY login_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<LoginLog> selectAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(1) FROM `login_log`")
    int countAll();
}
