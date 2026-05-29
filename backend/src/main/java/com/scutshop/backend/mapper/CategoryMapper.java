package com.scutshop.backend.mapper;

import com.scutshop.backend.model.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {
    @Select("SELECT * FROM `category` ORDER BY sort_order ASC, id ASC")
    List<Category> selectAll();

    @Select("SELECT * FROM `category` WHERE id = #{id}")
    Category selectById(@Param("id") Long id);

    @Select("SELECT * FROM `category` WHERE parent_id IS NULL ORDER BY sort_order ASC")
    List<Category> selectRootCategories();

    @Select("SELECT * FROM `category` WHERE parent_id = #{parentId} ORDER BY sort_order ASC")
    List<Category> selectByParentId(@Param("parentId") Long parentId);

    @Insert("INSERT INTO `category` (name, parent_id, sort_order) VALUES (#{name}, #{parentId}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category c);

    @Update("UPDATE `category` SET name = #{name}, parent_id = #{parentId}, sort_order = #{sortOrder} WHERE id = #{id}")
    int update(Category c);

    @Delete("DELETE FROM `category` WHERE id = #{id}")
    int delete(@Param("id") Long id);
}
