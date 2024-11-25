package com.qriz.sqld.oauth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.jwt.JwtVO;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.oauth.dto.SocialReqDto;
import com.qriz.sqld.oauth.dto.SocialRespDto;
import com.qriz.sqld.oauth.service.OAuth2Service;
import com.qriz.sqld.oauth.dto.OAuth2LoginResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    private final OAuth2Service oAuth2Service;

    @PostMapping("/social/login")
    public ResponseEntity<ResponseDto<SocialRespDto>> socialLogin(@RequestBody SocialReqDto socialReqDto) {
        try {
            // OAuth2 로그인 처리 및 토큰 발급
            OAuth2LoginResult loginResult = oAuth2Service.processOAuth2Login(socialReqDto);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.add(JwtVO.HEADER, JwtVO.TOKEN_PREFIX + loginResult.getAccessToken());
            headers.add(JwtVO.REFRESH_HEADER, JwtVO.TOKEN_PREFIX + loginResult.getRefreshToken());
            
            // 응답 데이터 생성
            SocialRespDto socialRespDto = SocialRespDto.fromUser(loginResult.getUser());
            
            log.debug("Social login successful - provider: {}, email: {}", 
                     socialReqDto.getProvider(), loginResult.getUser().getEmail());
            
            return new ResponseEntity<>(
                new ResponseDto<>(1, "소셜 로그인 성공", socialRespDto),
                headers,
                HttpStatus.OK
            );
        } catch (Exception e) {
            log.error("Social login failed", e);
            return new ResponseEntity<>(
                new ResponseDto<>(-1, "소셜 로그인 실패: " + e.getMessage(), null),
                HttpStatus.UNAUTHORIZED
            );
        }
    }
}