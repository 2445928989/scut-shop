package com.scutshop.backend.mapper;

import com.scutshop.backend.model.OperationLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OperationLogMapper {
    @Insert("INSERT INTO `operation_log` (user_id, username, action, target_type, target_id, details, ip_address) VALUES (#{userId}, #{username}, #{action}, #{targetType}, #{targetId}, #{details}, #{ipAddress})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog log);

    @Select("SELECT * FROM `operation_log` ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<OperationLog> selectAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM `operation_log` WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<OperationLog> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(1) FROM `operation_log`")
    int countAll();

    @Select("SELECT COUNT(1) FROM `operation_log` WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);
}
