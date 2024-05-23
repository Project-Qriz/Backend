package com.qriz.sqld.handler.logout;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import com.qriz.sqld.util.RedisUtil;

public class CustomLogoutHandler implements LogoutHandler {

    private final RedisUtil redisUtil;

    public CustomLogoutHandler(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            String username = authentication.getName();
            redisUtil.deleteData("RT:" + username);
            redisUtil.deleteData("AT:" + username);
        }
    }
}