package com.scutshop.backend.service;

import com.scutshop.backend.mapper.PaymentMapper;
import com.scutshop.backend.model.Payment;
import com.scutshop.backend.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final com.scutshop.backend.mapper.OrderMapper orderMapper;

    @Value("${PAYMENT_MOCK:true}")
    private boolean paymentMock;

    @Value("${PAYMENT_FORCE_FAILURE:false}")
    private boolean forceFailure;

    public PaymentService(PaymentMapper paymentMapper, com.scutshop.backend.mapper.OrderMapper orderMapper) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public void processPayment(Order order, String method) {
        // create payment record
        Payment p = new Payment();
        p.setOrderId(order.getId());
        p.setMethod(method == null ? "mock" : method);
        p.setAmount(order.getTotalAmount());
        p.setStatus(0);
        p.setTransactionNo(null);
        p.setPaidAt(null);
        paymentMapper.insertPayment(p);

        // simulate payment
        if (paymentMock) {
            if (forceFailure) {
                // mark failed
                p.setStatus(2);
                // no change to order
            } else {
                p.setStatus(1);
                p.setTransactionNo(UUID.randomUUID().toString());
                p.setPaidAt(LocalDateTime.now());
                // update order status
                orderMapper.updateOrderStatus(order.getId(), 1, 1);
            }
        } else {
            // in real implementation, call external gateway
            p.setStatus(2);
        }
        // persist is handled by insert only (no update implemented for payment table in
        // mapper)
    }
}
