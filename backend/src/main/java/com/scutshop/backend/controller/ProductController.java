package com.scutshop.backend.controller;

import com.scutshop.backend.dto.ProductRequest;
import com.scutshop.backend.model.Product;
import com.scutshop.backend.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {
    private final ProductService productService;
    private final com.scutshop.backend.service.UserService userService;

    public ProductController(ProductService productService, com.scutshop.backend.service.UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @GetMapping("/products")
    public ResponseEntity<?> list(@RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) Integer status) {
        List<Product> items = productService.search(q, page, size, status);
        int total = productService.count(q, status);
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        resp.put("total", total);
        resp.put("page", page);
        resp.put("size", size);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id,
            org.springframework.security.core.Authentication authentication) {
        Product p = productService.findById(id);
        if (p == null)
            return ResponseEntity.notFound().build();

        // Log browsing if user is authenticated
        if (authentication != null && authentication.isAuthenticated()) {
            var u = userService.findByUsername(authentication.getName());
            if (u != null) {
                userService.logAction(u.getId(), "BROWSE_PRODUCT",
                        "Product: " + p.getName() + " (ID: " + p.getId() + ")");
            }
        }
        return ResponseEntity.ok(p);
    }

    // Admin endpoints
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/products")
    public ResponseEntity<?> create(@RequestBody ProductRequest req) {
        Product p = new Product();
        p.setName(req.getName());
        p.setSku(req.getSku());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setStock(req.getStock());
        p.setCategoryId(req.getCategoryId());
        p.setImageUrl(req.getImageUrl());
        p.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        productService.create(p);
        return ResponseEntity.ok(Map.of("status", "created", "id", p.getId()));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/products/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody ProductRequest req,
            org.springframework.security.core.Authentication authentication) {
        Product p = productService.findById(id);
        if (p == null)
            return ResponseEntity.notFound().build();
        int oldStatus = p.getStatus() == null ? 1 : p.getStatus();
        if (req.getName() != null)
            p.setName(req.getName());
        if (req.getSku() != null)
            p.setSku(req.getSku());
        if (req.getDescription() != null)
            p.setDescription(req.getDescription());
        if (req.getPrice() != null)
            p.setPrice(req.getPrice());
        if (req.getStock() != null)
            p.setStock(req.getStock());
        if (req.getCategoryId() != null)
            p.setCategoryId(req.getCategoryId());
        if (req.getImageUrl() != null)
            p.setImageUrl(req.getImageUrl());
        if (req.getStatus() != null)
            p.setStatus(req.getStatus());
        productService.update(p);
        // if restored from 0 -> 1 record audit
        if (oldStatus == 0 && p.getStatus() == 1 && authentication != null && authentication.isAuthenticated()) {
            productService.restore(id, authentication.getName());
        }
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/products/{id}/restore")
    public ResponseEntity<?> restoreProduct(@PathVariable("id") Long id,
            org.springframework.security.core.Authentication authentication) {
        Product p = productService.findById(id);
        if (p == null)
            return ResponseEntity.notFound().build();
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        productService.restore(id, authentication.getName());
        return ResponseEntity.ok(Map.of("status", "restored", "id", id));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/admin/products/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id,
            org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        String actor = authentication.getName();
        int n = productService.delete(id, actor);
        if (n == 0)
            return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        return ResponseEntity.ok(Map.of("status", "deleted", "id", id));
    }
}
