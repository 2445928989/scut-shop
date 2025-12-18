package com.scutshop.backend.dto;

public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresInMinutes;
    private String refreshToken;

    public AuthResponse(String accessToken, long expiresInMinutes) {
        this.accessToken = accessToken;
        this.expiresInMinutes = expiresInMinutes;
    }

    public AuthResponse(String accessToken, long expiresInMinutes, String refreshToken) {
        this.accessToken = accessToken;
        this.expiresInMinutes = expiresInMinutes;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresInMinutes() {
        return expiresInMinutes;
    }

    public void setExpiresInMinutes(long expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
