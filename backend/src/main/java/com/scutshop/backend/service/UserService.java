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

    public int createUserWithRole(User user, String plainPassword, String roleName) {
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        if (user.getStatus() == null)
            user.setStatus(1);
        int n = userMapper.insert(user);
        Long roleId = userMapper.selectRoleIdByName("ROLE_USER");
        if (user.getId() != null && roleId != null) {
            userMapper.insertUserRole(user.getId(), roleId);
        }
        // assign additional role if specified and not ROLE_USER
        if (roleName != null && !"ROLE_USER".equals(roleName)) {
            Long extraRoleId = userMapper.selectRoleIdByName(roleName);
            if (extraRoleId != null && user.getId() != null) {
                userMapper.insertUserRole(user.getId(), extraRoleId);
            }
        }
        return n;
    }

    public void resetPassword(Long userId, String newPassword) {
        String hash = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(userId, hash);
    }

    public void logAction(Long userId, String action, String details) {
        UserLog log = new UserLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDetails(details);
        userLogMapper.insert(log);
    }

    public void logAction(Long userId, String action, String details, String ipAddress, Integer durationSeconds) {
        UserLog log = new UserLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setDurationSeconds(durationSeconds);
        userLogMapper.insert(log);
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

    public int disableUser(Long userId) {
        return userMapper.updateStatus(userId, 0);
    }

    public List<User> listAll(int page, int size) {
        return userMapper.selectAll(size, (page - 1) * size);
    }

    public int countAll() {
        return userMapper.countAll();
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
