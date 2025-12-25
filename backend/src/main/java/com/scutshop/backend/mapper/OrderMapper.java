package com.scutshop.backend.mapper;

import com.scutshop.backend.model.Order;
import com.scutshop.backend.model.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {
    @Insert("INSERT INTO `order` (user_id, order_no, shipping_address_id, total_amount, status, payment_status, remark) VALUES (#{userId}, #{orderNo}, #{shippingAddressId}, #{totalAmount}, #{status}, #{paymentStatus}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(Order order);

    @Insert("INSERT INTO `order_item` (order_id, product_id, product_name, price, quantity, subtotal) VALUES (#{orderId}, #{productId}, #{productName}, #{price}, #{quantity}, #{subtotal})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrderItem(OrderItem item);

    @Select("SELECT * FROM `order` WHERE id = #{id} LIMIT 1")
    Order selectById(@Param("id") Long id);

    @Select("SELECT * FROM `order_item` WHERE order_id = #{orderId}")
    List<OrderItem> selectItemsByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM `order` WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Order> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(1) FROM `order` WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM `order` ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Order> selectAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(1) FROM `order`")
    int countAll();

    @Update("UPDATE `order` SET status = #{newStatus}, payment_status = #{newPaymentStatus} WHERE id = #{orderId}")
    int updateOrderStatus(@Param("orderId") Long id, @Param("newStatus") Integer status,
            @Param("newPaymentStatus") Integer paymentStatus);

    @Select("SELECT SUM(total_amount) FROM `order` WHERE payment_status = 1")
    java.math.BigDecimal selectTotalSales();

    @Select("SELECT DATE(created_at) as date, SUM(total_amount) as amount FROM `order` WHERE payment_status = 1 GROUP BY DATE(created_at) ORDER BY date ASC LIMIT 30")
    List<java.util.Map<String, Object>> selectDailySales();

    @Select("SELECT product_name as name, SUM(quantity) as value FROM `order_item` oi JOIN `order` o ON oi.order_id = o.id WHERE o.payment_status = 1 GROUP BY product_id, product_name ORDER BY value DESC LIMIT 10")
    List<java.util.Map<String, Object>> selectTopProducts();
}
