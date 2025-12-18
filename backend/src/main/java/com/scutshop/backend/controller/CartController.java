package com.scutshop.backend.controller;

import com.scutshop.backend.dto.AddCartItemRequest;
import com.scutshop.backend.dto.UpdateCartItemRequest;
import com.scutshop.backend.model.Cart;
import com.scutshop.backend.model.CartItem;
import com.scutshop.backend.service.CartService;
import com.scutshop.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;
    private final UserService userService;

    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    // header X-Cart-Id: for guest carts
    @GetMapping("")
    public ResponseEntity<?> getCart(@RequestHeader(value = "X-Cart-Id", required = false) Long cartId,
            Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            var u = userService.findByUsername(authentication.getName());
            Cart c = cartService.getOrCreateCartForUser(u.getId());
            return ResponseEntity.ok(c);
        }
        if (cartId != null) {
            Cart c = cartService.getCartById(cartId);
            if (c == null)
                return ResponseEntity.notFound().build();
            return ResponseEntity.ok(c);
        }
        // create a new guest cart
        Cart c = cartService.getOrCreateCartForUser(null);
        return ResponseEntity.ok(Map.of("cartId", c.getId(), "items", c.getItems()));
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@RequestHeader(value = "X-Cart-Id", required = false) Long cartId,
            Authentication authentication,
            @RequestBody AddCartItemRequest req) {
        Long cid;
        if (authentication != null && authentication.isAuthenticated()) {
            var u = userService.findByUsername(authentication.getName());
            Cart c = cartService.getOrCreateCartForUser(u.getId());
            cid = c.getId();
        } else {
            if (cartId == null) {
                Cart c = cartService.getOrCreateCartForUser(null);
                cid = c.getId();
            } else
                cid = cartId;
        }
        CartItem it = cartService.addItem(cid, req.getProductId(), req.getQuantity());
        return ResponseEntity.ok(Map.of("status", "ok", "cartId", cid, "item", it));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @RequestBody UpdateCartItemRequest req) {
        cartService.updateItemQuantity(id, req.getQuantity());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        cartService.removeItem(id);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/merge")
    public ResponseEntity<?> mergeCart(@RequestParam("from") Long fromCartId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(401).build();
        var u = userService.findByUsername(authentication.getName());
        cartService.mergeCart(fromCartId, u.getId());
        return ResponseEntity.ok(Map.of("status", "merged"));
    }
}
