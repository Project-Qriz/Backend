package com.qriz.sqld.config.jwt;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.domain.preview.PreviewTestStatus;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserEnum;

@Component
public class JwtProcess {
    private static final Logger log = LoggerFactory.getLogger(JwtProcess.class);

    public static String createAccessToken(LoginUser loginUser) {
        String jwtToken = JWT.create()
                .withSubject("access_token")
                .withExpiresAt(new Date(System.currentTimeMillis() + JwtVO.ACCESS_TOKEN_EXPIRATION_TIME * 1000L))
                .withClaim("id", loginUser.getUser().getId())
                .withClaim("role", loginUser.getUser().getRole() + "")
                .withClaim("previewStatus",
                        loginUser.getUser().getPreviewTestStatus() != null
                                ? loginUser.getUser().getPreviewTestStatus().name()
                                : PreviewTestStatus.NOT_STARTED.name())
                .sign(Algorithm.HMAC512(JwtVO.SECRET));
        return JwtVO.TOKEN_PREFIX + jwtToken;
    }

    public static String createRefreshToken(LoginUser loginUser) {
        String jwtToken = JWT.create()
                .withSubject("refresh_token")
                .withExpiresAt(new Date(System.currentTimeMillis() + JwtVO.REFRESH_TOKEN_EXPIRATION_TIME * 1000L))
                .withClaim("id", loginUser.getUser().getId())
                .sign(Algorithm.HMAC512(JwtVO.SECRET));
        return JwtVO.TOKEN_PREFIX + jwtToken;
    }

    public static LoginUser verify(String token) {
        DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(JwtVO.SECRET)).build().verify(token);
        Long id = decodedJWT.getClaim("id").asLong();

        // Refresh Token인 경우 role claim이 없을 수 있음
        String subject = decodedJWT.getSubject();
        if ("refresh_token".equals(subject)) {
            User user = User.builder().id(id).build();
            return new LoginUser(user);
        }

        // Access Token인 경우 role claim 필요
        String role = decodedJWT.getClaim("role").asString();
        if (role == null) {
            throw new JWTVerificationException("Invalid token format");
        }

        String previewStatus = decodedJWT.getClaim("previewStatus").asString();

        User user = User.builder().id(id).role(UserEnum.valueOf(role))
                .previewTestStatus(PreviewTestStatus.valueOf(previewStatus)).build();
        return new LoginUser(user);
    }

    // 토큰의 유효성을 검사
    public static boolean isTokenValid(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC512(JwtVO.SECRET)).build().verify(token);

            // 토큰이 만료되었는지 확인
            if (jwt.getExpiresAt().before(new Date())) {
                return false;
            }

            // 토큰 타입 확인 (access_token 또는 refresh_token)
            String subject = jwt.getSubject();
            if (!"access_token".equals(subject) && !"refresh_token".equals(subject)) {
                return false;
            }

            // Access Token인 경우 role claim 확인
            if ("access_token".equals(subject)) {
                if (jwt.getClaim("role").isNull() || jwt.getClaim("previewStatus").isNull()) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Token validation failed: ", e);
            return false;
        }
    }

    // 토큰의 만료 시간이 30분 이내로 남았는지 확인
    public static boolean isTokenExpiringNear(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC512(JwtVO.SECRET)).build().verify(token);
            Date expiresAt = jwt.getExpiresAt();
            long timeUntilExpiration = expiresAt.getTime() - System.currentTimeMillis();
            return timeUntilExpiration < (30 * 60 * 1000); // 30분
        } catch (Exception e) {
            log.error("Token expiration check failed: ", e);
            return true;
        }
    }

    // 액세스 토큰 연장
    public static String extendAccessToken(LoginUser loginUser) {
        String jwtToken = JWT.create()
                .withSubject("access_token")
                .withExpiresAt(new Date(System.currentTimeMillis() + JwtVO.ACCESS_TOKEN_EXPIRATION_TIME * 1000L))
                .withClaim("id", loginUser.getUser().getId())
                .withClaim("role", loginUser.getUser().getRole() + "")
                .withClaim("previewStatus", loginUser.getUser().getPreviewTestStatus().name())
                .sign(Algorithm.HMAC512(JwtVO.SECRET));
        return JwtVO.TOKEN_PREFIX + jwtToken;
    }

    // 토큰에서 사용자 ID 추출
    public static Long getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC512(JwtVO.SECRET)).build().verify(token);
            return jwt.getClaim("id").asLong();
        } catch (Exception e) {
            log.error("Failed to extract user ID from token: ", e);
            throw new JWTVerificationException("Invalid token");
        }
    }
}