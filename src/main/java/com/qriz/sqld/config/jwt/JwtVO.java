package com.qriz.sqld.config.jwt;

public interface JwtVO {
    public static final String SECRET = "qriz"; // HS256 (대칭키)
    public static final long ACCESS_TOKEN_EXPIRATION_TIME = 60L * 60 * 6 * 1000; // 6시간 in milliseconds
    public static final long REFRESH_TOKEN_EXPIRATION_TIME = 60L * 60 * 24 * 60 * 1000; // 60일 in milliseconds
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER = "Authorization";
    public static final String REFRESH_HEADER = "Refresh-Token";
}