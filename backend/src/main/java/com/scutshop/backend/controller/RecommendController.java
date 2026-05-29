package com.scutshop.backend.controller;

import com.scutshop.backend.model.Product;
import com.scutshop.backend.service.RecommendService;
import com.scutshop.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {
    private final RecommendService recommendService;
    private final UserService userService;

    public RecommendController(RecommendService recommendService, UserService userService) {
        this.recommendService = recommendService;
        this.userService = userService;
    }

    @GetMapping("/together/{productId}")
    public ResponseEntity<?> frequentlyBoughtTogether(@PathVariable("productId") Long productId,
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        return ResponseEntity.ok(recommendService.frequentlyBoughtTogether(productId, limit));
    }

    @GetMapping("/user-cf")
    public ResponseEntity<?> userCF(org.springframework.security.core.Authentication auth,
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        if (auth == null || !auth.isAuthenticated()) {
            // return empty for unauthenticated users
            return ResponseEntity.ok(List.of());
        }
        var user = userService.findByUsername(auth.getName());
        if (user == null) return ResponseEntity.ok(List.of());
        List<Product> items = recommendService.userBasedCF(user.getId(), limit);
        return ResponseEntity.ok(items);
    }
}
