package com.scutshop.backend.mapper;

import com.scutshop.backend.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM \"user\" WHERE id = #{id} FETCH FIRST 1 ROWS ONLY")
    User selectById(@Param("id") Long id);

    @Select("SELECT * FROM \"user\" WHERE username = #{username} FETCH FIRST 1 ROWS ONLY")
    User selectByUsername(@Param("username") String username);

    @Insert("INSERT INTO \"user\" (username, email, password_hash, status) VALUES (#{username}, #{email}, #{passwordHash}, #{status})")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT id FROM \"role\" WHERE name = #{name} FETCH FIRST 1 ROWS ONLY")
    Long selectRoleIdByName(@Param("name") String name);

    // Use standard SQL; for H2 we can use MERGE to avoid duplicate key errors in
    // e2e profile
    @Insert("MERGE INTO \"user_role\" (user_id, role_id) KEY (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT r.name FROM \"role\" r JOIN \"user_role\" ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    java.util.List<String> selectRolesByUserId(@Param("userId") Long userId);
}
