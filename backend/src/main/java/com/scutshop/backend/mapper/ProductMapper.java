package com.scutshop.backend.mapper;

import com.scutshop.backend.model.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {
        @Select("SELECT * FROM `product` WHERE id = #{id} LIMIT 1")
        Product selectById(@Param("id") Long id);

        @Select({ "<script>",
                        "SELECT * FROM `product`",
                        "<where>",
                        "<if test='status != null and status &gt;= 0 and status != -1'>",
                        "  status = #{status}",
                        "</if>",
                        "<if test='q != null'>",
                        "  AND (name LIKE CONCAT('%',#{q},'%') OR description LIKE CONCAT('%',#{q},'%'))",
                        "</if>",
                        "</where>",
                        "ORDER BY created_at DESC",
                        "LIMIT #{limit} OFFSET #{offset}",
                        "</script>" })
        List<Product> search(@Param("q") String q, @Param("limit") int limit, @Param("offset") int offset,
                        @Param("status") Integer status);

        @Select({ "<script>",
                        "SELECT COUNT(1) FROM `product`",
                        "<where>",
                        "<if test='status != null and status &gt;= 0 and status != -1'>",
                        "  status = #{status}",
                        "</if>",
                        "<if test='q != null'>",
                        "  AND (name LIKE CONCAT('%',#{q},'%') OR description LIKE CONCAT('%',#{q},'%'))",
                        "</if>",
                        "</where>",
                        "</script>" })
        int count(@Param("q") String q, @Param("status") Integer status);

        @Insert("INSERT INTO `product` (name, sku, description, price, stock, image_url, status) VALUES (#{name}, #{sku}, #{description}, #{price}, #{stock}, #{imageUrl}, #{status})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(Product p);

        @Update("UPDATE `product` SET name=#{name}, sku=#{sku}, description=#{description}, price=#{price}, stock=#{stock}, image_url=#{imageUrl}, status=#{status}, updated_at=CURRENT_TIMESTAMP WHERE id=#{id}")
        int update(Product p);

        @Update("UPDATE `product` SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
        int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

        @Update("UPDATE `product` SET status = #{status}, updated_at=CURRENT_TIMESTAMP WHERE id = #{id}")
        int setStatus(@Param("id") Long id, @Param("status") int status);

        @Delete("DELETE FROM `product` WHERE id = #{id}")
        int delete(@Param("id") Long id);
}
