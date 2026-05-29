package com.scutshop.backend.controller;

import com.scutshop.backend.model.Category;
import com.scutshop.backend.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(value = "parentId", required = false) Long parentId) {
        if (parentId != null) {
            return ResponseEntity.ok(categoryService.listByParent(parentId));
        }
        return ResponseEntity.ok(categoryService.listAll());
    }

    @GetMapping("/roots")
    public ResponseEntity<?> roots() {
        return ResponseEntity.ok(categoryService.listRoot());
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SALES')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Category c) {
        categoryService.create(c);
        return ResponseEntity.ok(Map.of("status", "created", "id", c.getId()));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SALES')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Category c) {
        c.setId(id);
        categoryService.update(c);
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SALES')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
