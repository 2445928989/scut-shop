package com.scutshop.backend.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductAuditMapper {
    @Insert("INSERT INTO product_audit (product_id, action, actor, details, created_at) VALUES (#{productId}, #{action}, #{actor}, #{details}, CURRENT_TIMESTAMP)")
    int insert(@Param("productId") Long productId, @Param("action") String action, @Param("actor") String actor,
            @Param("details") String details);
}
