package com.qriz.sqld.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.qriz.sqld.service.TokenService;
import com.qriz.sqld.util.RedisUtil;
import com.qriz.sqld.util.TokenRefreshInterceptor;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final TokenService tokenService;
    private final RedisUtil redisUtil;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TokenRefreshInterceptor(tokenService, redisUtil))
                .addPathPatterns("/api/**");
    }
}
