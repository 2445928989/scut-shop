package com.scutshop.backend.service;

import com.scutshop.backend.mapper.UserMapper;
import com.scutshop.backend.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User findByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    public java.util.List<String> findRolesByUserId(Long userId) {
        return userMapper.selectRolesByUserId(userId);
    }

    public int createUser(User user, String plainPassword) {
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setStatus(1);
        int n = userMapper.insert(user);
        // assign ROLE_USER
        Long roleId = userMapper.selectRoleIdByName("ROLE_USER");
        if (roleId == null) {
            // create role if not exists
            // simple insert; in tests you may want to promote role management to migration
            // create role via SQL
            // For now, attempt to insert via SQL
            // (we'll run raw SQL to create role)
            java.util.Map<String, Object> m = new java.util.HashMap<>();
        }
        if (user.getId() != null && roleId != null) {
            userMapper.insertUserRole(user.getId(), roleId);
        }
        return n;
    }
}
