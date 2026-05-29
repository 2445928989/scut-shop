package com.scutshop.backend.controller;

import com.scutshop.backend.model.User;
import com.scutshop.backend.service.UserService;
import com.scutshop.backend.service.LoginLogService;
import com.scutshop.backend.service.OperationLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sales")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminSalesController {
    private final UserService userService;
    private final LoginLogService loginLogService;
    private final OperationLogService operationLogService;

    public AdminSalesController(UserService userService, LoginLogService loginLogService,
            OperationLogService operationLogService) {
        this.userService = userService;
        this.loginLogService = loginLogService;
        this.operationLogService = operationLogService;
    }

    @PostMapping
    public ResponseEntity<?> createSales(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        if (username == null || email == null || password == null)
            return ResponseEntity.badRequest().body(Map.of("error", "missing_fields"));
        User existing = userService.findByUsername(username);
        if (existing != null) return ResponseEntity.status(409).body(Map.of("error", "username_exists"));
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        userService.createUserWithRole(u, password, "ROLE_SALES");
        return ResponseEntity.ok(Map.of("status", "created", "id", u.getId()));
    }

    @GetMapping
    public ResponseEntity<?> listSalesUsers() {
        // return users with ROLE_SALES; for simplicity list all users
        List<User> users = userService.listAll(1, 200);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "missing_password"));
        User u = userService.findById(id);
        if (u == null) return ResponseEntity.status(404).body(Map.of("error", "user_not_found"));
        userService.resetPassword(id, newPassword);
        return ResponseEntity.ok(Map.of("status", "password_reset"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSales(@PathVariable("id") Long id) {
        User u = userService.findById(id);
        if (u == null) return ResponseEntity.status(404).body(Map.of("error", "user_not_found"));
        userService.disableUser(id);
        return ResponseEntity.ok(Map.of("status", "disabled"));
    }

    @GetMapping("/login-logs")
    public ResponseEntity<?> loginLogs(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return ResponseEntity.ok(loginLogService.listAll(page, size));
    }

    @GetMapping("/operation-logs")
    public ResponseEntity<?> operationLogs(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return ResponseEntity.ok(operationLogService.listAll(page, size));
    }
}
