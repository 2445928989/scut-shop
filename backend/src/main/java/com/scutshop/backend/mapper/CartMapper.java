package com.scutshop.backend.mapper;

import com.scutshop.backend.model.Cart;
import com.scutshop.backend.model.CartItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CartMapper {
    @Insert("INSERT INTO cart (user_id) VALUES (#{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCart(Cart cart);

    @Select("SELECT * FROM cart WHERE id = #{id}")
    Cart selectCartById(@Param("id") Long id);

    @Select("SELECT * FROM cart WHERE user_id = #{userId} LIMIT 1")
    Cart selectCartByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM cart_item WHERE cart_id = #{cartId}")
    List<CartItem> selectItemsByCartId(@Param("cartId") Long cartId);

    @Select("SELECT * FROM cart_item WHERE id = #{id} LIMIT 1")
    CartItem selectItemById(@Param("id") Long id);

    @Insert("INSERT INTO cart_item (cart_id, product_id, quantity, price) VALUES (#{cartId}, #{productId}, #{quantity}, #{price})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertItem(CartItem item);

    @Update("UPDATE cart_item SET quantity = #{quantity}, price = #{price} WHERE id = #{id}")
    int updateItem(CartItem item);

    @Delete("DELETE FROM cart_item WHERE id = #{id}")
    int deleteItem(@Param("id") Long id);

    @Delete("DELETE FROM cart_item WHERE cart_id = #{cartId}")
    int deleteItemsByCartId(@Param("cartId") Long cartId);

    @Update("UPDATE cart SET user_id = #{userId} WHERE id = #{cartId}")
    int assignCartToUser(@Param("cartId") Long cartId, @Param("userId") Long userId);
}