package com.qriz.sqld.config.jwt;

public interface JwtVO {
    public static final String SECRET = "qriz"; // HS256 (대칭키)
    public static final int ACCESS_TOKEN_EXPIRATION_TIME = 60 * 60 * 6; // 6 시간
    public static final int REFRESH_TOKEN_EXPIRATION_TIME = 60 * 60 * 24 * 30; // 7일
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER = "Authorization";
    public static final String REFRESH_HEADER = "Refresh-Token";
}