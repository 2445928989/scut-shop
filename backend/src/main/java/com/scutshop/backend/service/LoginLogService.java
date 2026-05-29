package com.scutshop.backend.service;

import com.scutshop.backend.mapper.LoginLogMapper;
import com.scutshop.backend.model.LoginLog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginLogService {
    private final LoginLogMapper mapper;

    public LoginLogService(LoginLogMapper mapper) {
        this.mapper = mapper;
    }

    public void record(Long userId, String username, String ipAddress, String userAgent) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        mapper.insert(log);
    }

    public List<LoginLog> listByUser(Long userId, int page, int size) {
        return mapper.selectByUserId(userId, size, (page - 1) * size);
    }

    public List<LoginLog> listAll(int page, int size) {
        return mapper.selectAll(size, (page - 1) * size);
    }

    public int countAll() {
        return mapper.countAll();
    }
}
