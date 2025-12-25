package com.scutshop.backend.service;

import com.scutshop.backend.mapper.OrderMapper;
import com.scutshop.backend.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final ProductService productService;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final UserService userService;

    public OrderService(OrderMapper orderMapper, CartService cartService, ProductService productService,
            PaymentService paymentService, EmailService emailService, UserService userService) {
        this.orderMapper = orderMapper;
        this.cartService = cartService;
        this.productService = productService;
        this.paymentService = paymentService;
        this.emailService = emailService;
        this.userService = userService;
    }

    @Transactional
    public Order checkout(Long userId, Long shippingAddressId, String paymentMethod) {
        // get user cart
        Cart cart = cartService.getOrCreateCartForUser(userId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty())
            throw new IllegalArgumentException("cart_empty");

        // check stock and compute total
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem it : cart.getItems()) {
            Product p = productService.findById(it.getProductId());
            if (p == null)
                throw new IllegalArgumentException("product_not_found");
            if (p.getStock() < it.getQuantity())
                throw new IllegalArgumentException("insufficient_stock");
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
        }

        // create order
        Order o = new Order();
        o.setUserId(userId);
        o.setOrderNo(UUID.randomUUID().toString());
        o.setShippingAddressId(shippingAddressId);
        o.setTotalAmount(total);
        o.setStatus(0);
        o.setPaymentStatus(0);
        orderMapper.insertOrder(o);

        // create order items and deduct stock
        for (CartItem it : cart.getItems()) {
            Product p = productService.findById(it.getProductId());
            OrderItem oi = new OrderItem();
            oi.setOrderId(o.getId());
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setPrice(p.getPrice());
            oi.setQuantity(it.getQuantity());
            oi.setSubtotal(p.getPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
            orderMapper.insertOrderItem(oi);

            int dec = productService.decrementStock(p.getId(), it.getQuantity());
            if (dec <= 0)
                throw new IllegalStateException("stock_decrement_failed");
        }

        // clear cart
        cartService.mergeCart(cart.getId(), null); // deletes items

        // create payment record and process
        paymentService.processPayment(o, paymentMethod);

        // Log purchase
        userService.logAction(userId, "PURCHASE", "Order No: " + o.getOrderNo() + ", Total: ¥" + o.getTotalAmount());

        // send email to user (best-effort)
        try {
            var user = userService.findById(userId);
            if (user != null) {
                emailService.sendPaymentConfirmation(user.getEmail(), o);
            }
        } catch (Exception ex) {
            // do not fail checkout for email/user lookup errors
            System.err.println("Warning: failed to send payment email: " + ex.getMessage());
        }

        return o;
    }

    public Order getById(Long id) {
        Order o = orderMapper.selectById(id);
        if (o != null) {
            o.setItems(orderMapper.selectItemsByOrderId(o.getId()));
        }
        return o;
    }

    public List<Order> listByUser(Long userId, int page, int size) {
        int limit = size;
        int offset = (Math.max(1, page) - 1) * size;
        List<Order> orders = orderMapper.selectByUserId(userId, limit, offset);
        for (Order o : orders) {
            o.setItems(orderMapper.selectItemsByOrderId(o.getId()));
        }
        return orders;
    }

    public int countByUser(Long userId) {
        return orderMapper.countByUserId(userId);
    }

    public List<Order> listAll(int page, int size) {
        int offset = (page - 1) * size;
        List<Order> orders = orderMapper.selectAll(size, offset);
        for (Order o : orders) {
            o.setItems(orderMapper.selectItemsByOrderId(o.getId()));
        }
        return orders;
    }

    public int countAll() {
        return orderMapper.countAll();
    }

    @Transactional
    public void updateStatus(Long orderId, Integer status, Integer paymentStatus) {
        System.out.println(
                "OrderService.updateStatus: id=" + orderId + ", status=" + status + ", paymentStatus=" + paymentStatus);
        orderMapper.updateOrderStatus(orderId, status, paymentStatus);
        System.out.println("OrderService.updateStatus: success");
    }

    public java.util.Map<String, Object> getSalesStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalSales", orderMapper.selectTotalSales());
        stats.put("totalOrders", orderMapper.countAll());
        stats.put("dailySales", orderMapper.selectDailySales());
        stats.put("topProducts", orderMapper.selectTopProducts());
        return stats;
    }
}
