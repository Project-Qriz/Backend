package com.qriz.sqld.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.qriz.sqld.handler.oauth.RestTemplateErrorHandler;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
            // 연결 타임아웃 설정
            .setConnectTimeout(Duration.ofSeconds(5))
            // 읽기 타임아웃 설정
            .setReadTimeout(Duration.ofSeconds(5))
            // 에러 핸들러 추가
            .errorHandler(new RestTemplateErrorHandler())
            .build();
    }
}