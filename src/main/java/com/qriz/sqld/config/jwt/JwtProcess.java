package com.qriz.sqld.config.jwt;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserEnum;

@Component
public class JwtProcess {

    private static final Logger log = LoggerFactory.getLogger(JwtProcess.class);

    // 토큰 생성
    public static String createAccessToken(LoginUser loginUser) {
        long expirationTime = System.currentTimeMillis() + JwtVO.ACCESS_TOKEN_EXPIRATION_TIME * 1000L;
        log.debug("디버깅 : AccessToken 만료 시간 (초 단위): " + expirationTime);

        String jwtToken = JWT.create()
                .withSubject("access_token")
                .withExpiresAt(new Date(expirationTime))
                .withClaim("id", loginUser.getUser().getId())
                .withClaim("role", loginUser.getUser().getRole() + "")
                .sign(Algorithm.HMAC512(JwtVO.SECRET));
        return JwtVO.TOKEN_PREFIX + jwtToken;
    }

    public static String createRefreshToken(LoginUser loginUser) {
        long expirationTime = System.currentTimeMillis() + JwtVO.REFRESH_TOKEN_EXPIRATION_TIME * 1000L;
        log.debug("디버깅 : RefreshToken 만료 시간 (밀리초 단위): " + expirationTime);

        String jwtToken = JWT.create()
                .withSubject("refresh_token")
                .withExpiresAt(new Date(expirationTime))
                .withClaim("id", loginUser.getUser().getId())
                .sign(Algorithm.HMAC512(JwtVO.SECRET));
        return JwtVO.TOKEN_PREFIX + jwtToken;
    }

    public static LoginUser verify(String token) {
        DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(JwtVO.SECRET)).build().verify(token);
        Long id = decodedJWT.getClaim("id").asLong();
        String role = decodedJWT.getClaim("role").asString();
        User user = User.builder().id(id).role(UserEnum.valueOf(role)).build();
        return new LoginUser(user);
    }

    public static boolean isTokenExpired(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC512(JwtVO.SECRET)).build().verify(token);
            return jwt.getExpiresAt().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public static String getUsernameFromToken(String token) {
        DecodedJWT jwt = JWT.require(Algorithm.HMAC512(JwtVO.SECRET)).build().verify(token);
        return jwt.getClaim("id").asString();
    }
}
