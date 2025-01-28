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
    private String provider;    // 소셜 로그인 제공자
    private String email;       // 사용자 이메일
    private String nickname;    // 사용자 닉네임
    private String previewStatus; // 설문조사 및 프리뷰 테스트 여부
    
    public static SocialRespDto fromUser(User user) {
        return SocialRespDto.builder()
                .provider(user.getProvider())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .previewStatus(user.getPreviewTestStatus().name())
                .build();
    }
}