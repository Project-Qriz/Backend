package com.qriz.sqld.oauth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.oauth.dto.SocialReqDto;
import com.qriz.sqld.oauth.dto.SocialRespDto;
import com.qriz.sqld.oauth.service.OAuth2Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class AuthController {
    private final OAuth2Service oAuth2Service;

    @PostMapping("/verifyToken")
    public ResponseEntity<ResponseDto<SocialRespDto>> verifyToken(@RequestBody SocialReqDto socialReqDto) {
        String accessToken = oAuth2Service.processOAuth2Login(socialReqDto);
        SocialRespDto socialRespDto = new SocialRespDto();
        socialRespDto.setProvider(socialReqDto.getProvider());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, accessToken);

        return new ResponseEntity<>(new ResponseDto<>(1, "소셜 로그인 성공", socialRespDto), headers, HttpStatus.OK);
    }
}
