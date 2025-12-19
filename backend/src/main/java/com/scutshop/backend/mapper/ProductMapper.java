package com.scutshop.backend.mapper;

import com.scutshop.backend.model.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {
        @Select("SELECT * FROM \"product\" WHERE id = #{id} FETCH FIRST 1 ROWS ONLY")
        Product selectById(@Param("id") Long id);

        @Select({ "<script>",
                        "SELECT * FROM \"product\"",
                        "<where>",
                        "<if test='q != null'>",
                        "  (name LIKE CONCAT('%',#{q},'%') OR description LIKE CONCAT('%',#{q},'%'))",
                        "</if>",
                        "</where>",
                        "ORDER BY created_at DESC",
                        "OFFSET #{offset} ROWS FETCH NEXT #{limit} ROWS ONLY",
                        "</script>" })
        List<Product> search(@Param("q") String q, @Param("limit") int limit, @Param("offset") int offset);

        @Select({ "<script>",
                        "SELECT COUNT(1) FROM \"product\"",
                        "<where>",
                        "<if test='q != null'>",
                        "  (name LIKE CONCAT('%',#{q},'%') OR description LIKE CONCAT('%',#{q},'%'))",
                        "</if>",
                        "</where>",
                        "</script>" })
        int count(@Param("q") String q);

        @Insert("INSERT INTO \"product\" (name, sku, description, price, stock, category_id, image_url, status) VALUES (#{name}, #{sku}, #{description}, #{price}, #{stock}, #{categoryId}, #{imageUrl}, #{status})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(Product p);

        @Update("UPDATE \"product\" SET name=#{name}, sku=#{sku}, description=#{description}, price=#{price}, stock=#{stock}, category_id=#{categoryId}, image_url=#{imageUrl}, status=#{status}, updated_at=CURRENT_TIMESTAMP WHERE id=#{id}")
        int update(Product p);

        @Update("UPDATE \"product\" SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
        int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

        @Delete("DELETE FROM \"product\" WHERE id = #{id}")
        int delete(@Param("id") Long id);
}
