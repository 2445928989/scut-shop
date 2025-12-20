package com.scutshop.backend.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMapper {
    @Insert("INSERT INTO `payment` (order_id, method, amount, status, transaction_no, paid_at) VALUES (#{orderId}, #{method}, #{amount}, #{status}, #{transactionNo}, #{paidAt})")
    int insertPayment(com.scutshop.backend.model.Payment p);
}
