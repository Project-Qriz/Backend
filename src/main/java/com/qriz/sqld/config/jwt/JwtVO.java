package com.qriz.sqld.config.jwt;

public interface JwtVO {
    public static final String SECRET = "abo2"; // HS256 (대칭키)
    public static final int EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 5; // 5일
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER = "Authorization";
}
