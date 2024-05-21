package com.qriz.sqld.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.token.TokenReqDto;
import com.qriz.sqld.dto.token.TokenRespDto;
import com.qriz.sqld.service.TokenService;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/token")
public class TokenController {
    
    private final TokenService tokenService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody TokenReqDto tokenReqDto) {
        try {
            TokenRespDto tokenRespDto = tokenService.refreshJwtAccessToken(tokenReqDto.getRefreshToken());
            return new ResponseEntity<>(new ResponseDto<>(1, "엑세스 토큰 갱신", null), HttpStatus.OK);
        } catch (AuthenticationException a) {
            return new ResponseEntity<>(new ResponseDto<>(-1, "유효하지 않은 Refresh Token", null), HttpStatus.UNAUTHORIZED);
        }
    }
    
}
