package com.qriz.sqld.oauth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialReqDto {
    private String provider;    // "google", "kakao" 등
    private String authCode;    // ID Token 또는 Access Token
    private String platform;    // "android" 또는 "ios"
}