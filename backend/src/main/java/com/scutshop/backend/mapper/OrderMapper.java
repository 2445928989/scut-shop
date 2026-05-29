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

    @Select({ "<script>",
            "SELECT product_name as name, SUM(quantity) as value FROM `order_item` oi JOIN `order` o ON oi.order_id = o.id WHERE o.payment_status = 1",
            "<if test='startDate != null'> AND o.created_at &gt;= #{startDate}</if>",
            "<if test='endDate != null'> AND o.created_at &lt;= #{endDate}</if>",
            "<if test='categoryId != null'> AND oi.product_id IN (SELECT id FROM product WHERE category_id = #{categoryId})</if>",
            "GROUP BY product_id, product_name ORDER BY value DESC LIMIT #{limit}",
            "</script>" })
    List<java.util.Map<String, Object>> selectTopProductsFiltered(@Param("limit") int limit,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("categoryId") Long categoryId);

    @Select("SELECT DATE(created_at) as date, SUM(total_amount) as amount FROM `order` WHERE payment_status = 1 AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(created_at) ORDER BY date ASC")
    List<java.util.Map<String, Object>> selectDailySalesRange(@Param("days") int days);

    @Select({ "<script>",
            "SELECT DATE(created_at) as date, SUM(total_amount) as amount FROM `order` WHERE payment_status = 1",
            "<if test='startDate != null'> AND created_at &gt;= #{startDate}</if>",
            "<if test='endDate != null'> AND created_at &lt;= #{endDate}</if>",
            "GROUP BY DATE(created_at) ORDER BY date ASC",
            "</script>" })
    List<java.util.Map<String, Object>> selectDailySalesBetween(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT YEARWEEK(created_at, 1) as week, SUM(total_amount) as amount FROM `order` WHERE payment_status = 1 AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{weeks} WEEK) GROUP BY YEARWEEK(created_at, 1) ORDER BY week ASC")
    List<java.util.Map<String, Object>> selectWeeklySales(@Param("weeks") int weeks);

    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m') as month, SUM(total_amount) as amount FROM `order` WHERE payment_status = 1 AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{months} MONTH) GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month ASC")
    List<java.util.Map<String, Object>> selectMonthlySales(@Param("months") int months);

    @Select("SELECT DATE(created_at) as date, COUNT(1) as count FROM `order` WHERE payment_status = 1 AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(created_at) ORDER BY date ASC")
    List<java.util.Map<String, Object>> selectDailyOrderCount(@Param("days") int days);

    @Select("SELECT DISTINCT order_id FROM `order_item` WHERE product_id = #{productId}")
    List<Long> selectOrderIdsByProductId(@Param("productId") Long productId);
}
