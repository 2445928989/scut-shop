package com.scutshop.backend.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface PaymentMapper {
    @Insert("INSERT INTO `payment` (order_id, method, amount, status, transaction_no, paid_at) VALUES (#{orderId}, #{method}, #{amount}, #{status}, #{transactionNo}, #{paidAt})")
    int insertPayment(com.scutshop.backend.model.Payment p);

    @Update("UPDATE `payment` SET status = #{status}, transaction_no = #{transactionNo}, paid_at = #{paidAt} WHERE order_id = #{orderId}")
    int updatePayment(com.scutshop.backend.model.Payment p);
}
