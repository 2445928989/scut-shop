package com.scutshop.backend.controller;

import com.scutshop.backend.model.Address;
import com.scutshop.backend.service.AddressService;
import com.scutshop.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressService addressService;
    private final UserService userService;

    public AddressController(AddressService addressService, UserService userService) {
        this.addressService = addressService;
        this.userService = userService;
    }

    private Long getUserId(org.springframework.security.core.Authentication auth) {
        var u = userService.findByUsername(auth.getName());
        return u != null ? u.getId() : null;
    }

    @GetMapping
    public ResponseEntity<?> list(org.springframework.security.core.Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(addressService.listByUser(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Address addr, org.springframework.security.core.Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).build();
        addr.setUserId(userId);
        addressService.create(addr);
        return ResponseEntity.ok(Map.of("status", "created", "id", addr.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Address addr,
            org.springframework.security.core.Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).build();
        addr.setId(id);
        addr.setUserId(userId);
        addressService.update(addr);
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id, org.springframework.security.core.Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).build();
        int n = addressService.delete(id, userId);
        if (n == 0) return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
