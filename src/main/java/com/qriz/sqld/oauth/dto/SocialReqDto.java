package com.qriz.sqld.oauth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialReqDto {
    private String provider;
    private String authCode;
}
