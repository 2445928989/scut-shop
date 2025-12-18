package com.scutshop.backend.service;

import com.scutshop.backend.mapper.RefreshTokenMapper;
import com.scutshop.backend.model.RefreshToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenMapper mapper;

    @Value("${jwt.refresh-expires-days:7}")
    private int refreshExpiresDays;

    public RefreshTokenService(RefreshTokenMapper mapper) {
        this.mapper = mapper;
    }

    public String createRefreshToken(Long userId) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUserId(userId);
        rt.setExpiresAt(LocalDateTime.now().plusDays(refreshExpiresDays));
        rt.setRevoked(false);
        mapper.insert(rt);
        return rt.getToken();
    }

    public RefreshToken verify(String token) {
        RefreshToken rt = mapper.selectByToken(token);
        if (rt == null)
            return null;
        if (rt.getRevoked() != null && rt.getRevoked())
            return null;
        if (rt.getExpiresAt() != null && rt.getExpiresAt().isBefore(LocalDateTime.now()))
            return null;
        return rt;
    }

    public void revokeByToken(String token) {
        mapper.revokeByToken(token);
    }

    public void revokeAllForUser(Long userId) {
        mapper.deleteByUserId(userId);
    }
}
