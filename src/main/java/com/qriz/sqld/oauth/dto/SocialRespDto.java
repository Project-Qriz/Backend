package com.qriz.sqld.oauth.dto;

import com.qriz.sqld.domain.user.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialRespDto {
    private String provider;
    
    public void SocialReqDto(User user) {
        this.provider = user.getProvider();
    }
}
