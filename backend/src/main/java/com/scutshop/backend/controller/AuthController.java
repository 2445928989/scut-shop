package com.scutshop.backend.controller;

import com.scutshop.backend.model.User;
import com.scutshop.backend.security.JwtTokenProvider;
import com.scutshop.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final org.springframework.security.authentication.AuthenticationManager authenticationManager;

    private final com.scutshop.backend.service.RefreshTokenService refreshTokenService;
    private final com.scutshop.backend.service.EmailService emailService;

    public AuthController(UserService userService, JwtTokenProvider tokenProvider,
            org.springframework.security.authentication.AuthenticationManager authenticationManager,
            com.scutshop.backend.service.RefreshTokenService refreshTokenService,
            com.scutshop.backend.service.EmailService emailService) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);
        log.debug("Register called with body: {}", body);
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        if (username == null || email == null || password == null) {
            log.debug("Missing fields in register request: {}", body);
            return ResponseEntity.badRequest().body(Map.of("error", "missing_fields"));
        }
        User existing = userService.findByUsername(username);
        if (existing != null) {
            log.debug("Username exists: {}", username);
            return ResponseEntity.status(409).body(Map.of("error", "username_exists"));
        }
        User existingEmail = userService.findByEmail(email);
        if (existingEmail != null) {
            log.debug("Email exists: {}", email);
            return ResponseEntity.status(409).body(Map.of("error", "email_exists"));
        }
        try {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            boolean activationEnabled = "true".equals(System.getenv("EMAIL_ACTIVATION_ENABLED"));
            if (activationEnabled)
                u.setStatus(0);
            userService.createUser(u, password);
            log.info("User registered: {}", username);
            if (activationEnabled) {
                // generate activation token and send email
                User created = userService.findByUsername(username);
                String token = java.util.UUID.randomUUID().toString();
                java.time.LocalDateTime expires = java.time.LocalDateTime.now().plusHours(24);
                userService.setActivation(created.getId(), token, expires);
                String frontendBase = java.util.Optional.ofNullable(System.getenv("FRONTEND_BASE"))
                        .orElse("http://localhost:3000");
                String link = frontendBase + "/activate?token=" + token;
                emailService.sendActivationEmail(email, link);
                return ResponseEntity.ok(Map.of("status", "registered", "activation", "sent"));
            }
            return ResponseEntity.ok(Map.of("status", "registered"));
        } catch (Exception ex) {
            log.error("Error during registration for {}: {}", username, ex.toString(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "server_error"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody com.scutshop.backend.dto.AuthRequest request) {
        try {
            var authToken = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    request.getUsername(), request.getPassword());
            authenticationManager.authenticate(authToken);
            java.util.List<String> roles = userService
                    .findRolesByUserId(userService.findByUsername(request.getUsername()).getId());
            String token = tokenProvider.generateToken(request.getUsername(), roles);
            long expires = Long.parseLong(java.util.Optional.ofNullable(System.getenv("JWT_EXPIRES_MIN")).orElse("60"));

            // create refresh token
            Long userId = userService.findByUsername(request.getUsername()).getId();
            String refresh = refreshTokenService.createRefreshToken(userId);

            return ResponseEntity.ok(new com.scutshop.backend.dto.AuthResponse(token, expires, refresh));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody com.scutshop.backend.dto.RefreshRequest request) {
        if (request == null || request.getRefreshToken() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_refresh_token"));
        }
        var rt = refreshTokenService.verify(request.getRefreshToken());
        if (rt == null)
            return ResponseEntity.status(401).body(Map.of("error", "invalid_refresh_token"));
        // generate new access token and rotate refresh token
        var user = userService.findById(rt.getUserId());
        if (user == null)
            return ResponseEntity.status(401).body(Map.of("error", "user_not_found"));
        var username = user.getUsername();
        var roles = userService.findRolesByUserId(user.getId());
        String token = tokenProvider.generateToken(username, roles);
        // rotate: revoke old and create new
        refreshTokenService.revokeByToken(request.getRefreshToken());
        String newRefresh = refreshTokenService.createRefreshToken(user.getId());
        long expires = Long.parseLong(java.util.Optional.ofNullable(System.getenv("JWT_EXPIRES_MIN")).orElse("60"));
        return ResponseEntity.ok(new com.scutshop.backend.dto.AuthResponse(token, expires, newRefresh));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) com.scutshop.backend.dto.RefreshRequest request,
            org.springframework.security.core.Authentication authentication) {
        if (request != null && request.getRefreshToken() != null) {
            refreshTokenService.revokeByToken(request.getRefreshToken());
            return ResponseEntity.ok(Map.of("status", "logged_out"));
        }
        if (authentication != null && authentication.isAuthenticated()) {
            // revoke all tokens for the current user
            var u = userService.findByUsername(authentication.getName());
            if (u != null) {
                refreshTokenService.revokeAllForUser(u.getId());
            }
            return ResponseEntity.ok(Map.of("status", "logged_out"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "no_token_or_user"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity
                .ok(Map.of("username", authentication.getName(), "authorities", authentication.getAuthorities()));
    }

    @GetMapping("/activate")
    public ResponseEntity<?> activate(@RequestParam("token") String token) {
        if (token == null || token.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "missing_token"));
        User u = userService.findByActivationToken(token);
        if (u == null)
            return ResponseEntity.status(404).body(Map.of("error", "invalid_token"));
        if (u.getActivationExpires() != null && u.getActivationExpires().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.status(400).body(Map.of("error", "token_expired"));
        }
        userService.activateUser(u.getId());
        return ResponseEntity.ok(Map.of("status", "activated"));
    }
}
