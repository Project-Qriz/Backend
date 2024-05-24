package com.qriz.sqld.handler.logout;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import com.qriz.sqld.util.RedisUtil;

public class CustomLogoutHandler implements LogoutHandler {

    private final RedisUtil redisUtil;
    private final Logger log = LoggerFactory.getLogger(CustomLogoutHandler.class);

    public CustomLogoutHandler(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        log.debug("CustomLogoutHandler 호출됨");
        try {
            if (authentication != null && authentication.getName() != null) {
                String username = authentication.getName();
                log.debug("로그아웃 사용자: " + username);
                redisUtil.deleteData("RT:" + username);
                redisUtil.deleteData("AT:" + username);
                log.debug("Redis에서 토큰 삭제: RT:" + username + " 및 AT:" + username);
            } else {
                log.debug("로그아웃 중 인증 정보 없음");
            }
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생", e);
        }
    }
}