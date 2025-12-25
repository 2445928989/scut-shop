package com.scutshop.backend.mapper;

import com.scutshop.backend.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM `user` WHERE id = #{id} LIMIT 1")
    User selectById(@Param("id") Long id);

    @Select("SELECT * FROM `user` WHERE username = #{username} LIMIT 1")
    User selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM `user` WHERE email = #{email} LIMIT 1")
    User selectByEmail(@Param("email") String email);

    @Insert("INSERT INTO `user` (username, email, password_hash, status, activation_token, activation_expires) VALUES (#{username}, #{email}, #{passwordHash}, #{status}, #{activationToken}, #{activationExpires})")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT * FROM `user` WHERE activation_token = #{token} LIMIT 1")
    User selectByActivationToken(@Param("token") String token);

    @org.apache.ibatis.annotations.Update("UPDATE `user` SET activation_token = #{token}, activation_expires = #{expires} WHERE id = #{id}")
    int updateActivation(@Param("id") Long id, @Param("token") String token,
            @Param("expires") java.time.LocalDateTime expires);

    @org.apache.ibatis.annotations.Update("UPDATE `user` SET status = #{status}, activation_token = NULL, activation_expires = NULL WHERE id = #{id}")
    int updateStatusAndClearToken(@Param("id") Long id, @Param("status") int status);

    @Select("SELECT id FROM `role` WHERE name = #{name} LIMIT 1")
    Long selectRoleIdByName(@Param("name") String name);

    // Use INSERT IGNORE for MySQL to avoid duplicate key errors when adding roles
    @Insert("INSERT IGNORE INTO `user_role` (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT r.name FROM `role` r JOIN `user_role` ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    java.util.List<String> selectRolesByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM `user` ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    java.util.List<User> selectAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(1) FROM `user`")
    int countAll();
}
