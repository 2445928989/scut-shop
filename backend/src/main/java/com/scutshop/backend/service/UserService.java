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
        if (user.getStatus() == null)
            user.setStatus(1);
        int n = userMapper.insert(user);
        // assign ROLE_USER
        Long roleId = userMapper.selectRoleIdByName("ROLE_USER");
        if (roleId == null) {
            // role not found; in dev/test environments this may be fine.
        }
        if (user.getId() != null && roleId != null) {
            userMapper.insertUserRole(user.getId(), roleId);
        }
        return n;
    }

    public User findByActivationToken(String token) {
        return userMapper.selectByActivationToken(token);
    }

    public int setActivation(Long userId, String token, java.time.LocalDateTime expires) {
        return userMapper.updateActivation(userId, token, expires);
    }

    public int activateUser(Long userId) {
        return userMapper.updateStatusAndClearToken(userId, 1);
    }
}
