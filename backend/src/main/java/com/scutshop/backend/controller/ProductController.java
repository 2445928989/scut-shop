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

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ResponseEntity<?> list(@RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        List<Product> items = productService.search(q, page, size);
        int total = productService.count(q);
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        resp.put("total", total);
        resp.put("page", page);
        resp.put("size", size);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        Product p = productService.findById(id);
        if (p == null)
            return ResponseEntity.notFound().build();
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
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody ProductRequest req) {
        Product p = productService.findById(id);
        if (p == null)
            return ResponseEntity.notFound().build();
        p.setName(req.getName());
        p.setSku(req.getSku());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setStock(req.getStock());
        p.setCategoryId(req.getCategoryId());
        p.setImageUrl(req.getImageUrl());
        p.setStatus(req.getStatus() == null ? p.getStatus() : req.getStatus());
        productService.update(p);
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/admin/products/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        productService.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
