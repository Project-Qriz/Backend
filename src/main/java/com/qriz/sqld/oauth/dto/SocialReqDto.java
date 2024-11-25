package com.qriz.sqld.oauth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialReqDto {
    private String provider;    // "google", "kakao" 등
    private String authCode;    // OAuth 인증 코드
}
