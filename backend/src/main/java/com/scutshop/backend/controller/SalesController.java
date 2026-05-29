package com.scutshop.backend.controller;

import com.scutshop.backend.model.Product;
import com.scutshop.backend.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SALES')")
public class SalesController {
    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;

    public SalesController(ProductService productService, OrderService orderService, UserService userService) {
        this.productService = productService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/products")
    public ResponseEntity<?> listProducts(@RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) Integer status) {
        List<Product> items = productService.search(q, page, size, status);
        int total = productService.count(q, status);
        return ResponseEntity.ok(Map.of("items", items, "total", total, "page", page, "size", size));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Product p = productService.findById(id);
        if (p == null) return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        if (body.containsKey("price")) p.setPrice(new java.math.BigDecimal(body.get("price").toString()));
        if (body.containsKey("stock")) p.setStock(Integer.valueOf(body.get("stock").toString()));
        if (body.containsKey("status")) p.setStatus(Integer.valueOf(body.get("status").toString()));
        productService.update(p);
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    @GetMapping("/orders")
    public ResponseEntity<?> listOrders(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        List<com.scutshop.backend.model.Order> orders = orderService.listAll(page, size);
        int total = orderService.countAll();
        return ResponseEntity.ok(Map.of("items", orders, "total", total, "page", page, "size", size));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        Map<String, Object> stats = orderService.getSalesStats();
        return ResponseEntity.ok(stats);
    }
}
