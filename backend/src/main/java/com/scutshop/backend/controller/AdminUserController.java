package com.scutshop.backend.controller;

import com.scutshop.backend.model.User;
import com.scutshop.backend.model.UserLog;
import com.scutshop.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("")
    public ResponseEntity<?> listUsers(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        List<User> users = userService.listAll(page, size);
        int total = userService.countAll();
        return ResponseEntity.ok(Map.of("items", users, "total", total, "page", page, "size", size));
    }

    @GetMapping("/logs")
    public ResponseEntity<?> listLogs(@RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        List<UserLog> logs = userService.listLogs(userId, page, size);
        int total = userService.countLogs(userId);
        return ResponseEntity.ok(Map.of("items", logs, "total", total, "page", page, "size", size));
    }
}
