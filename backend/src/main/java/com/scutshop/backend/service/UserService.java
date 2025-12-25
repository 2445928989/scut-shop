package com.scutshop.backend.service;

import com.scutshop.backend.mapper.UserLogMapper;
import com.scutshop.backend.mapper.UserMapper;
import com.scutshop.backend.model.User;
import com.scutshop.backend.model.UserLog;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final UserLogMapper userLogMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper, UserLogMapper userLogMapper) {
        this.userMapper = userMapper;
        this.userLogMapper = userLogMapper;
    }

    public User findByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    public User findByEmail(String email) {
        return userMapper.selectByEmail(email);
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

    public List<User> listAll(int page, int size) {
        return userMapper.selectAll(size, (page - 1) * size);
    }

    public int countAll() {
        return userMapper.countAll();
    }

    public void logAction(Long userId, String action, String details) {
        UserLog log = new UserLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDetails(details);
        userLogMapper.insert(log);
    }

    public List<UserLog> listLogs(Long userId, int page, int size) {
        if (userId != null) {
            return userLogMapper.selectByUserId(userId, size, (page - 1) * size);
        }
        return userLogMapper.selectAll(size, (page - 1) * size);
    }

    public int countLogs(Long userId) {
        if (userId != null) {
            return userLogMapper.countByUserId(userId);
        }
        return userLogMapper.countAll();
    }
}
