package com.scutshop.backend.controller;

import com.scutshop.backend.dto.CheckoutRequest;
import com.scutshop.backend.model.Order;
import com.scutshop.backend.service.OrderService;
import com.scutshop.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final UserService userService;

    public static class UpdateStatusRequest {
        private Integer status;
        private Integer paymentStatus;

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Integer getPaymentStatus() {
            return paymentStatus;
        }

        public void setPaymentStatus(Integer paymentStatus) {
            this.paymentStatus = paymentStatus;
        }
    }

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest req, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(401).build();
        var u = userService.findByUsername(authentication.getName());
        try {
            Order o = orderService.checkout(u.getId(), req.getAddressId(), req.getPaymentMethod());
            return ResponseEntity.ok(Map.of("status", "created", "orderId", o.getId(), "orderNo", o.getOrderNo()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(401).build();
        var u = userService.findByUsername(authentication.getName());
        Order o = orderService.getById(id);
        if (o == null)
            return ResponseEntity.notFound().build();
        if (!o.getUserId().equals(u.getId()))
            return ResponseEntity.status(403).build();
        return ResponseEntity.ok(o);
    }

    @GetMapping("")
    public ResponseEntity<?> list(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(401).build();
        var u = userService.findByUsername(authentication.getName());
        var items = orderService.listByUser(u.getId(), page, size);
        var total = orderService.countByUser(u.getId());
        return ResponseEntity.ok(Map.of("items", items, "total", total, "page", page, "size", size));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listAll(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        var items = orderService.listAll(page, size);
        var total = orderService.countAll();
        return ResponseEntity.ok(Map.of("items", items, "total", total, "page", page, "size", size));
    }

    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable("id") Long id,
            @RequestBody UpdateStatusRequest req) {
        try {
            System.out.println("Updating order status: id=" + id + ", status=" + req.getStatus() + ", paymentStatus="
                    + req.getPaymentStatus());
            if (req.getStatus() == null || req.getPaymentStatus() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "missing_fields"));
            }
            System.out.println("Fetching order with id: " + id);
            Order o = orderService.getById(id);
            if (o == null) {
                System.out.println("Order not found: " + id);
                return ResponseEntity.notFound().build();
            }
            System.out.println("Order found: " + o.getOrderNo());
            orderService.updateStatus(id, req.getStatus(), req.getPaymentStatus());
            return ResponseEntity.ok(Map.of("status", "updated"));
        } catch (Exception e) {
            System.err.println("Error updating order status:");
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStatistics() {
        return ResponseEntity.ok(orderService.getSalesStats());
    }
}
