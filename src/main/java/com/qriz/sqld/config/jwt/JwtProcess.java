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

    private final Logger log = LoggerFactory.getLogger(getClass());

    // 토큰 생성
    public static String createAccessToken(LoginUser loginUser) {
        String jwtToken = JWT.create()
                .withSubject("access_token")
                .withExpiresAt(new Date(System.currentTimeMillis() + JwtVO.ACCESS_TOKEN_EXPIRATION_TIME))
                .withClaim("id", loginUser.getUser().getId())
                .withClaim("role", loginUser.getUser().getRole() + "")
                .sign(Algorithm.HMAC512(JwtVO.SECRET));
        return JwtVO.TOKEN_PREFIX + jwtToken;
    }

    public static String createRefreshToken(LoginUser loginUser) {
        String jwtToken = JWT.create()
                .withSubject("refresh_token")
                .withExpiresAt(new Date(System.currentTimeMillis() + JwtVO.REFRESH_TOKEN_EXPIRATION_TIME))
                .withClaim("id", loginUser.getUser().getId())
                .sign(Algorithm.HMAC512(JwtVO.SECRET));
        return JwtVO.TOKEN_PREFIX + jwtToken;
    }

    // 토큰 검증 (return 되는 LoginUser 객체를 강제로 시큐리티 세션에 직접 주입)
    public static LoginUser verify(String token) {
        DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(JwtVO.SECRET)).build().verify(token);
        Long id = decodedJWT.getClaim("id").asLong();
        String role = decodedJWT.getClaim("role").asString();
        User user = User.builder().id(id).role(UserEnum.valueOf(role)).build();
        LoginUser loginUser = new LoginUser(user);
        return loginUser;
    }
}
