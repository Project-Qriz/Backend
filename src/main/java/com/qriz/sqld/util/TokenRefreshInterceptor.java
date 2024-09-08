package com.qriz.sqld.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

import com.qriz.sqld.config.jwt.JwtProcess;
import com.qriz.sqld.config.jwt.JwtVO;
import com.qriz.sqld.service.TokenService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TokenRefreshInterceptor implements HandlerInterceptor {
    
    private final TokenService tokenService;
    private final RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String accessToken = request.getHeader(JwtVO.HEADER);
        if (accessToken != null && accessToken.startsWith(JwtVO.TOKEN_PREFIX)) {
            accessToken = accessToken.replace(JwtVO.TOKEN_PREFIX, "");
            
            if (JwtProcess.isTokenExpired(accessToken)) {
                String username = JwtProcess.getUsernameFromToken(accessToken);
                String refreshToken = (String) redisUtil.getData("RT:" + username);
                
                if (refreshToken != null && !JwtProcess.isTokenExpired(refreshToken)) {
                    String newAccessToken = tokenService.refreshJwtAccessToken(refreshToken).getAccessToken();
                    response.setHeader(JwtVO.HEADER, newAccessToken);
                }
            }
        }

        return true;
    }
}
