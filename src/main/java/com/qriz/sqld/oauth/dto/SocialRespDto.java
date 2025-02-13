package com.qriz.sqld.oauth.dto;

import com.qriz.sqld.domain.user.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialRespDto {
    private String name;    // 사용자 닉네임
    
    public static SocialRespDto fromUser(User user) {
        return SocialRespDto.builder()
                .name(user.getNickname())
                .build();
    }
}