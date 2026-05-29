package com.scutshop.backend.service;

import com.scutshop.backend.mapper.OperationLogMapper;
import com.scutshop.backend.model.OperationLog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogService {
    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void record(Long userId, String username, String action, String targetType, Long targetId, String details, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        mapper.insert(log);
    }

    public List<OperationLog> listAll(int page, int size) {
        return mapper.selectAll(size, (page - 1) * size);
    }

    public List<OperationLog> listByUser(Long userId, int page, int size) {
        return mapper.selectByUserId(userId, size, (page - 1) * size);
    }

    public int countAll() { return mapper.countAll(); }
    public int countByUser(Long userId) { return mapper.countByUserId(userId); }
}
