package com.scutshop.backend.mapper;

import com.scutshop.backend.model.Address;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AddressMapper {
    @Select("SELECT * FROM `address` WHERE id = #{id}")
    Address selectById(@Param("id") Long id);

    @Select("SELECT * FROM `address` WHERE user_id = #{userId} ORDER BY is_default DESC, created_at DESC")
    List<Address> selectByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO `address` (user_id, recipient, phone, province, city, district, detail, is_default) VALUES (#{userId}, #{recipient}, #{phone}, #{province}, #{city}, #{district}, #{detail}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Address a);

    @Update("UPDATE `address` SET recipient = #{recipient}, phone = #{phone}, province = #{province}, city = #{city}, district = #{district}, detail = #{detail}, is_default = #{isDefault} WHERE id = #{id} AND user_id = #{userId}")
    int update(Address a);

    @Delete("DELETE FROM `address` WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE `address` SET is_default = 0 WHERE user_id = #{userId}")
    int clearDefault(@Param("userId") Long userId);
}
